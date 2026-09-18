package eu.kanade.tachiyomi.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress

// DNS-over-HTTPS providers serving specific regions; the global ones live in DohProviders.kt.

/** Resolves names through AliDNS's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohAliDNS(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://dns.alidns.com/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("223.5.5.5"),
            InetAddress.getByName("223.6.6.6"),
            InetAddress.getByName("2400:3200::1"),
            InetAddress.getByName("2400:3200:baba::1"),
        )
        .build(),
)

/** Resolves names through DNSPod's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohDNSPod(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://doh.pub/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("1.12.12.12"),
            InetAddress.getByName("120.53.53.53"),
        )
        .build(),
)

/** Resolves names through 360's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.doh360(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://doh.360.cn/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("101.226.4.6"),
            InetAddress.getByName("218.30.118.6"),
            InetAddress.getByName("123.125.81.6"),
            InetAddress.getByName("140.207.198.6"),
            InetAddress.getByName("180.163.249.75"),
            InetAddress.getByName("101.199.113.208"),
            InetAddress.getByName("36.99.170.86"),
        )
        .build(),
)

/** Resolves names through Quad 101's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohQuad101(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://dns.twnic.tw/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("101.101.101.101"),
            InetAddress.getByName("2001:de4::101"),
            InetAddress.getByName("2001:de4::102"),
        )
        .build(),
)

/** Resolves names through Njalla's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohNajalla(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://dns.njal.la/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("95.215.19.53"),
            InetAddress.getByName("2001:67c:2354:2::53"),
        )
        .build(),
)

/**
 * Source: https://shecan.ir/
 */
public fun OkHttpClient.Builder.dohShecan(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://free.shecan.ir/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("178.22.122.100"),
            InetAddress.getByName("185.51.200.2"),
        )
        .build(),
)
