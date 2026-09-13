package eu.kanade.tachiyomi.network

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress

/**
 * Based on https://github.com/square/okhttp/blob/ef5d0c83f7bbd3a0c0534e7ca23cbc4ee7550f3b/okhttp-dnsoverhttps/src/test/java/okhttp3/dnsoverhttps/DohProviders.java
 */

public const val PREF_DOH_CLOUDFLARE: Int = 1

/** Preference value selecting Google as the DNS-over-HTTPS provider. */
public const val PREF_DOH_GOOGLE: Int = 2

/** Preference value selecting AdGuard as the DNS-over-HTTPS provider. */
public const val PREF_DOH_ADGUARD: Int = 3

/** Preference value selecting Quad9 as the DNS-over-HTTPS provider. */
public const val PREF_DOH_QUAD9: Int = 4

/** Preference value selecting AliDNS as the DNS-over-HTTPS provider. */
public const val PREF_DOH_ALIDNS: Int = 5

/** Preference value selecting DNSPod as the DNS-over-HTTPS provider. */
public const val PREF_DOH_DNSPOD: Int = 6

/** Preference value selecting 360 as the DNS-over-HTTPS provider. */
public const val PREF_DOH_360: Int = 7

/** Preference value selecting Quad 101 as the DNS-over-HTTPS provider. */
public const val PREF_DOH_QUAD101: Int = 8

/** Preference value selecting Mullvad as the DNS-over-HTTPS provider. */
public const val PREF_DOH_MULLVAD: Int = 9

/** Preference value selecting Control D as the DNS-over-HTTPS provider. */
public const val PREF_DOH_CONTROLD: Int = 10

/** Preference value selecting Njalla as the DNS-over-HTTPS provider. */
public const val PREF_DOH_NJALLA: Int = 11

/** Preference value selecting Shecan as the DNS-over-HTTPS provider. */
public const val PREF_DOH_SHECAN: Int = 12

/** Resolves names through Cloudflare's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohCloudflare(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("162.159.36.1"),
            InetAddress.getByName("162.159.46.1"),
            InetAddress.getByName("1.1.1.1"),
            InetAddress.getByName("1.0.0.1"),
            InetAddress.getByName("162.159.132.53"),
            InetAddress.getByName("2606:4700:4700::1111"),
            InetAddress.getByName("2606:4700:4700::1001"),
            InetAddress.getByName("2606:4700:4700::0064"),
            InetAddress.getByName("2606:4700:4700::6400"),
        )
        .build(),
)

/** Resolves names through Google's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohGoogle(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://dns.google/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("8.8.4.4"),
            InetAddress.getByName("8.8.8.8"),
            InetAddress.getByName("2001:4860:4860::8888"),
            InetAddress.getByName("2001:4860:4860::8844"),
        )
        .build(),
)

// AdGuard "Default" DNS works too but for the sake of making sure no site is blacklisted,
// we use "Unfiltered"

/** Resolves names through AdGuard's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohAdGuard(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://dns-unfiltered.adguard.com/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("94.140.14.140"),
            InetAddress.getByName("94.140.14.141"),
            InetAddress.getByName("2a10:50c0::1:ff"),
            InetAddress.getByName("2a10:50c0::2:ff"),
        )
        .build(),
)

/** Resolves names through Quad9's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohQuad9(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://dns.quad9.net/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("9.9.9.9"),
            InetAddress.getByName("149.112.112.112"),
            InetAddress.getByName("2620:fe::fe"),
            InetAddress.getByName("2620:fe::9"),
        )
        .build(),
)

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

/*
 * Mullvad DoH
 * without ad blocking option
 * Source: https://mullvad.net/en/help/dns-over-https-and-dns-over-tls
 */

/** Resolves names through Mullvad's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohMullvad(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url(" https://dns.mullvad.net/dns-query".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("194.242.2.2"),
            InetAddress.getByName("2a07:e340::2"),
        )
        .build(),
)

/*
 * Control D
 * unfiltered option
 * Source: https://controld.com/free-dns/?
 */

/** Resolves names through Control D's DNS-over-HTTPS endpoint. */
public fun OkHttpClient.Builder.dohControlD(): OkHttpClient.Builder = dns(
    DnsOverHttps.Builder().client(build())
        .url("https://freedns.controld.com/p0".toHttpUrl())
        .bootstrapDnsHosts(
            InetAddress.getByName("76.76.2.0"),
            InetAddress.getByName("76.76.10.0"),
            InetAddress.getByName("2606:1a40::"),
            InetAddress.getByName("2606:1a40:1::"),
        )
        .build(),
)

/*
 * Njalla
 * Non logging and uncensored
 */

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
