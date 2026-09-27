package mihon.app.shizuku

import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageInstaller
import android.os.IBinder
import android.os.IInterface
import android.os.ParcelFileDescriptor
import android.os.UserHandle
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import rikka.shizuku.SystemServiceHelper
import java.io.File
import java.io.FileDescriptor
import java.lang.reflect.Proxy

/** Stands in for the hidden `IPackageInstaller`: the service only reaches it through reflection. */
internal class FakeInstaller(private val session: FakeSession) {
    val created = mutableListOf<String>()
    var params: PackageInstaller.SessionParams? = null

    fun createSession(params: PackageInstaller.SessionParams, installer: String, attribution: String, user: Int): Int {
        created += "4:$installer:$attribution:$user"
        this.params = params
        return SESSION_ID
    }

    fun createSession(params: PackageInstaller.SessionParams, installer: String, user: Int): Int {
        created += "3:$installer:$user"
        this.params = params
        return SESSION_ID
    }

    fun openSession(id: Int): Any {
        check(id == SESSION_ID)
        return delegating(session, "android.content.pm.IPackageInstallerSession", LegacySession::class.java)
    }
}

/** The pre-Android-12 `createSession`, absent from the runtime's `IPackageInstaller`. */
internal interface LegacyInstaller {
    fun createSession(params: PackageInstaller.SessionParams, installer: String, user: Int): Int
}

/** The pre-Oreo `commit`, absent from the runtime's `IPackageInstallerSession`. */
internal interface LegacySession {
    fun commit(sender: IntentSender)
}

/** A proxy implementing the hidden [hidden] interface (plus [extra]) by calling [target]'s same-named method. */
internal fun delegating(target: Any, hidden: String, extra: Class<*>): Any {
    val type = Class.forName(hidden)
    return Proxy.newProxyInstance(type.classLoader, arrayOf(type, extra)) { _, method, args ->
        target::class.java.getMethod(method.name, *method.parameterTypes).invoke(target, *args.orEmpty())
    }
}

/** Stands in for the hidden `IPackageInstallerSession`, writing the apk into [target]. */
internal class FakeSession(private val target: File) {
    val commits = mutableListOf<String>()

    fun openWrite(name: String, offset: Long, length: Long): ParcelFileDescriptor {
        commits += "write:$name:$offset:$length"
        return ParcelFileDescriptor.open(
            target,
            ParcelFileDescriptor.MODE_WRITE_ONLY or ParcelFileDescriptor.MODE_CREATE,
        )
    }

    fun commit(sender: IntentSender, transferred: Boolean) {
        commits += "commit:${sender::class.simpleName}:$transferred"
    }

    fun commit(sender: IntentSender) {
        commits += "commit:${sender::class.simpleName}"
    }
}

internal const val SESSION_ID = 7

/**
 * Serves [installer] as the package service (through a proxy of the hidden `IPackageManager`) and a
 * system `ActivityThread` whose shell context is the test application.
 */
internal fun installShellFakes(installer: FakeInstaller) {
    val pmClass = Class.forName("android.content.pm.IPackageManager")
    val pm = Proxy.newProxyInstance(pmClass.classLoader, arrayOf(pmClass)) { _, method, _ ->
        if (method.name == "getPackageInstaller") {
            delegating(installer, "android.content.pm.IPackageInstaller", LegacyInstaller::class.java)
        } else {
            null
        }
    }
    val binder = mockk<IBinder>()
    every { binder.queryLocalInterface(any()) } returns pm as IInterface
    mockkStatic(SystemServiceHelper::class)
    every { SystemServiceHelper.getSystemService("package") } returns binder

    val app = ApplicationProvider.getApplicationContext<Context>()
    val shell = mockk<Context>()
    every { shell.createPackageContext("com.android.shell", 0) } returns app
    val contextImpl = Class.forName("android.app.ContextImpl")
    val system = mockkClass(contextImpl.kotlin)
    val asUser = contextImpl.getMethod(
        "createPackageContextAsUser",
        String::class.java,
        Int::class.java,
        UserHandle::class.java,
    )
    val flags = Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
    val user = UserHandle::class.java.getConstructor(Int::class.java).newInstance(0)
    every { asUser.invoke(system, "com.android.shell", flags, user) } returns shell

    val threadClass = Class.forName("android.app.ActivityThread")
    val thread = mockkClass(threadClass.kotlin)
    every { threadClass.getMethod("getSystemContext").invoke(thread) } returns system
    mockkStatic(threadClass.kotlin)
    every { threadClass.getMethod("systemMain").invoke(null) } returns thread
}

/** Acknowledges every FileBridge command without a bridge on the other end. */
internal fun stubIoBridge() {
    val bridge = Class.forName("libcore.io.IoBridge")
    val params = arrayOf(FileDescriptor::class.java, ByteArray::class.java, Int::class.java, Int::class.java)
    val write = bridge.getMethod("write", *params)
    val read = bridge.getMethod("read", *params)
    mockkStatic(bridge.kotlin)
    every { write.invoke(null, any<FileDescriptor>(), any<ByteArray>(), any<Int>(), any<Int>()) } returns Unit
    every { read.invoke(null, any<FileDescriptor>(), any<ByteArray>(), any<Int>(), any<Int>()) } returns 8
}
