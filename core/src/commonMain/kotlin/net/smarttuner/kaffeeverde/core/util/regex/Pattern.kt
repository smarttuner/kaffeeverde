package net.smarttuner.kaffeeverde.core.util.regex

class Pattern {
    companion object{
        /**
         * Returns a literal pattern `String` for the specified
         * `String`.
         *
         *
         * This method produces a `String` that can be used to
         * create a `Pattern` that would match the string
         * `s` as if it were a literal pattern. Metacharacters
         * or escape sequences in the input sequence will be given no special
         * meaning.
         *
         * @param  s The string to be literalized
         * @return  A literal string replacement
         * @since 1.5
         */
        fun quote(s: String): String? {
            var slashEIndex = s.indexOf("\\E")
            if (slashEIndex == -1) return "\\Q$s\\E"
            val sb = StringBuilder(s.length * 2)
            sb.append("\\Q")
            slashEIndex = 0
            var current = 0
            while (s.indexOf("\\E", current).also { slashEIndex = it } != -1) {
                sb.append(s.substring(current, slashEIndex))
                current = slashEIndex + 2
                sb.append("\\E\\\\E\\Q")
            }
            sb.append(s.substring(current, s.length))
            sb.append("\\E")
            return sb.toString()
        }
    }
}