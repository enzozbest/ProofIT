package kcl.seg.rtt.chat_history

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import org.owasp.encoder.Encode

fun sanitise(input: String): String{
    var cleaned = Jsoup.clean(input, Safelist.none())
    cleaned = Encode.forHtml(cleaned)
    print("Cleaned is $cleaned")
    return cleaned.trim()
}
