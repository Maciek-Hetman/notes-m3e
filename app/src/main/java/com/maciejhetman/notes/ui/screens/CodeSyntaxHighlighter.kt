package com.maciejhetman.notes.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Colors used to paint syntax-highlighted tokens inside fenced code blocks.
 */
/**
 * Colors used to paint syntax-highlighted tokens inside fenced code blocks.
 */
data class CodeHighlightColors(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val background: Color? = null,
    val textColor: Color? = null
)

fun resolveSyntaxThemeColors(
    theme: com.maciejhetman.notes.data.SyntaxTheme,
    isDark: Boolean = false,
    fallbackPrimary: Color,
    fallbackSecondary: Color,
    fallbackTertiary: Color,
    fallbackOnSurface: Color,
    fallbackSurfaceVariant: Color
): CodeHighlightColors {
    return when (theme) {
        com.maciejhetman.notes.data.SyntaxTheme.MATERIAL -> CodeHighlightColors(
            keyword = fallbackPrimary,
            string = fallbackTertiary,
            number = fallbackSecondary,
            comment = fallbackOnSurface.copy(alpha = 0.45f),
            background = fallbackSurfaceVariant,
            textColor = fallbackOnSurface
        )
        com.maciejhetman.notes.data.SyntaxTheme.MONOKAI -> CodeHighlightColors(
            keyword = Color(0xFFF92672),
            string = Color(0xFFA6E22E),
            number = Color(0xFFAE81FF),
            comment = Color(0xFF75715E),
            background = Color(0xFF272822),
            textColor = Color(0xFFF8F8F2)
        )
        com.maciejhetman.notes.data.SyntaxTheme.DRACULA -> CodeHighlightColors(
            keyword = Color(0xFFFF79C6),
            string = Color(0xFFF1FA8C),
            number = Color(0xFFBD93F9),
            comment = Color(0xFF6272A4),
            background = Color(0xFF282A36),
            textColor = Color(0xFFF8F8F2)
        )
        com.maciejhetman.notes.data.SyntaxTheme.SOLARIZED -> if (isDark) {
            CodeHighlightColors(
                keyword = Color(0xFF859900),
                string = Color(0xFF2AA198),
                number = Color(0xFFD33682),
                comment = Color(0xFF586E75),
                background = Color(0xFF002B36),
                textColor = Color(0xFF839496)
            )
        } else {
            CodeHighlightColors(
                keyword = Color(0xFF859900),
                string = Color(0xFF2AA198),
                number = Color(0xFFD33682),
                comment = Color(0xFF93A1A1),
                background = Color(0xFFFDF6E3),
                textColor = Color(0xFF657B83)
            )
        }
        com.maciejhetman.notes.data.SyntaxTheme.GITHUB -> if (isDark) {
            CodeHighlightColors(
                keyword = Color(0xFFFF7B72),
                string = Color(0xFFA5D6FF),
                number = Color(0xFF79C0FF),
                comment = Color(0xFF8B949E),
                background = Color(0xFF161B22),
                textColor = Color(0xFFC9D1D9)
            )
        } else {
            CodeHighlightColors(
                keyword = Color(0xFFCF222E),
                string = Color(0xFF0A3069),
                number = Color(0xFF0550AE),
                comment = Color(0xFF6E7781),
                background = Color(0xFFF6F8FA),
                textColor = Color(0xFF24292F)
            )
        }
        com.maciejhetman.notes.data.SyntaxTheme.NORD -> CodeHighlightColors(
            keyword = Color(0xFF81A1C1),
            string = Color(0xFFA3BE8C),
            number = Color(0xFFB48EAD),
            comment = Color(0xFF616E88),
            background = Color(0xFF2E3440),
            textColor = Color(0xFFD8DEE9)
        )
    }
}



private data class LanguageSyntax(
    val lineComments: List<String> = emptyList(),
    val blockComments: List<Pair<String, String>> = emptyList(),
    val keywords: Set<String> = emptySet()
)

// Fallback used for unknown/unspecified languages — covers the most common comment/string
// conventions so an unlabeled code block still gets some highlighting.
private val GENERIC_SYNTAX = LanguageSyntax(
    lineComments = listOf("//", "#"),
    blockComments = listOf("/*" to "*/")
)

private val LANGUAGE_ALIASES = mapOf(
    "js" to "javascript", "jsx" to "javascript", "mjs" to "javascript",
    "ts" to "typescript", "tsx" to "typescript",
    "py" to "python", "py3" to "python",
    "sh" to "bash", "shell" to "bash", "zsh" to "bash", "console" to "bash", "terminal" to "bash",
    "kt" to "kotlin", "kts" to "kotlin",
    "yml" to "yaml",
    "c++" to "cpp", "cxx" to "cpp",
    "cs" to "csharp", "c#" to "csharp",
    "rb" to "ruby",
    "rs" to "rust",
    "htm" to "html",
    "objc" to "objectivec", "objective-c" to "objectivec"
)

private val C_STYLE_KEYWORDS = setOf(
    "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
    "return", "true", "false", "null", "void", "const", "static", "public", "private",
    "protected", "class", "struct", "enum", "new", "this", "super", "try", "catch", "finally",
    "throw", "import", "package", "extends", "implements", "interface"
)

private val LANGUAGE_SYNTAX = mapOf(
    "bash" to LanguageSyntax(
        lineComments = listOf("#"),
        keywords = setOf(
            "if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done",
            "case", "esac", "function", "return", "exit", "export", "local", "readonly",
            "echo", "read", "in", "select", "sudo", "cd", "set", "unset", "shift", "break", "continue"
        )
    ),
    "python" to LanguageSyntax(
        lineComments = listOf("#"),
        keywords = setOf(
            "def", "class", "if", "elif", "else", "for", "while", "try", "except", "finally",
            "with", "as", "import", "from", "return", "yield", "lambda", "pass", "break",
            "continue", "raise", "in", "is", "not", "and", "or", "None", "True", "False",
            "global", "nonlocal", "assert", "async", "await", "self", "del"
        )
    ),
    "javascript" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = C_STYLE_KEYWORDS + setOf(
            "function", "var", "let", "const", "async", "await", "yield", "typeof",
            "instanceof", "in", "of", "delete", "export", "default", "undefined", "NaN"
        )
    ),
    "typescript" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = C_STYLE_KEYWORDS + setOf(
            "function", "var", "let", "const", "async", "await", "yield", "typeof",
            "instanceof", "in", "of", "delete", "export", "default", "undefined", "NaN",
            "type", "as", "readonly", "namespace", "declare", "abstract", "implements"
        )
    ),
    "kotlin" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = setOf(
            "fun", "val", "var", "if", "else", "for", "while", "do", "when", "is", "as",
            "in", "!in", "!is", "return", "break", "continue", "class", "object", "interface",
            "package", "import", "null", "true", "false", "this", "super", "try", "catch",
            "finally", "throw", "companion", "override", "private", "public", "protected",
            "internal", "open", "sealed", "data", "enum", "abstract", "suspend", "inline",
            "vararg", "lateinit", "by", "init", "constructor", "typealias", "it"
        )
    ),
    "java" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = C_STYLE_KEYWORDS + setOf(
            "int", "long", "short", "byte", "float", "double", "boolean", "char", "String",
            "abstract", "final", "synchronized", "volatile", "transient", "instanceof", "assert"
        )
    ),
    "c" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = setOf(
            "if", "else", "for", "while", "do", "switch", "case", "default", "break",
            "continue", "return", "int", "long", "short", "float", "double", "char", "void",
            "const", "static", "struct", "union", "enum", "typedef", "sizeof", "unsigned",
            "signed", "extern", "goto", "include", "define"
        )
    ),
    "cpp" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = C_STYLE_KEYWORDS + setOf(
            "int", "long", "short", "float", "double", "char", "bool", "void", "namespace",
            "using", "template", "typename", "virtual", "override", "nullptr", "auto",
            "constexpr", "friend", "operator", "delete"
        )
    ),
    "csharp" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = C_STYLE_KEYWORDS + setOf(
            "using", "namespace", "int", "string", "bool", "var", "async", "await", "get",
            "set", "override", "virtual", "sealed", "readonly", "params", "out", "ref"
        )
    ),
    "go" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = setOf(
            "func", "package", "import", "var", "const", "type", "struct", "interface",
            "if", "else", "for", "range", "switch", "case", "default", "return", "break",
            "continue", "go", "defer", "chan", "select", "map", "true", "false", "nil"
        )
    ),
    "rust" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = setOf(
            "fn", "let", "mut", "if", "else", "for", "while", "loop", "match", "return",
            "break", "continue", "struct", "enum", "impl", "trait", "pub", "use", "mod",
            "crate", "self", "Self", "super", "true", "false", "None", "Some", "Ok", "Err",
            "async", "await", "move", "ref", "where", "dyn", "static", "const"
        )
    ),
    "ruby" to LanguageSyntax(
        lineComments = listOf("#"),
        keywords = setOf(
            "def", "end", "if", "elsif", "else", "unless", "while", "until", "for", "in",
            "do", "class", "module", "self", "return", "yield", "begin", "rescue", "ensure",
            "raise", "true", "false", "nil", "require", "attr_accessor", "puts", "then"
        )
    ),
    "php" to LanguageSyntax(
        lineComments = listOf("//", "#"),
        blockComments = listOf("/*" to "*/"),
        keywords = C_STYLE_KEYWORDS + setOf(
            "function", "echo", "namespace", "use", "array", "foreach", "as", "require",
            "require_once", "include", "include_once", "public", "private", "protected",
            "abstract", "final", "instanceof"
        )
    ),
    "sql" to LanguageSyntax(
        lineComments = listOf("--"),
        blockComments = listOf("/*" to "*/"),
        keywords = setOf(
            "select", "insert", "update", "delete", "from", "where", "join", "inner",
            "left", "right", "outer", "on", "group", "by", "order", "having", "limit",
            "into", "values", "set", "table", "create", "drop", "alter", "index", "primary",
            "key", "foreign", "references", "not", "null", "and", "or", "as", "distinct",
            "union", "all", "exists", "in", "like", "between", "case", "when", "then", "end"
        )
    ),
    "swift" to LanguageSyntax(
        lineComments = listOf("//"),
        blockComments = listOf("/*" to "*/"),
        keywords = setOf(
            "func", "var", "let", "if", "else", "for", "while", "switch", "case", "default",
            "return", "break", "continue", "class", "struct", "enum", "protocol", "extension",
            "import", "guard", "true", "false", "nil", "self", "super", "init", "deinit",
            "private", "public", "internal", "fileprivate", "static", "override", "throws",
            "try", "catch", "async", "await", "in"
        )
    ),
    "yaml" to LanguageSyntax(
        lineComments = listOf("#"),
        keywords = setOf("true", "false", "null", "yes", "no")
    ),
    "json" to LanguageSyntax(
        keywords = setOf("true", "false", "null")
    )
)

private fun resolveSyntax(language: String): LanguageSyntax {
    if (language.isBlank()) return GENERIC_SYNTAX
    val normalized = language.trim().lowercase()
    val resolved = LANGUAGE_ALIASES[normalized] ?: normalized
    return LANGUAGE_SYNTAX[resolved] ?: GENERIC_SYNTAX
}

private fun isIdentifierStart(c: Char) = c.isLetter() || c == '_'
private fun isIdentifierPart(c: Char) = c.isLetterOrDigit() || c == '_'

/**
 * Tokenizes [code] for [language] and layers keyword/string/number/comment colors on top of
 * whatever base style already covers the range [startOffset, startOffset + code.length).
 *
 * Uses a single left-to-right scan (rather than independent regex passes) so that comments and
 * strings correctly take priority over their contents — e.g. a "#" inside a string literal is
 * never mistaken for the start of a comment.
 */
fun AnnotatedString.Builder.applySyntaxHighlighting(
    code: String,
    language: String,
    startOffset: Int,
    colors: CodeHighlightColors
) {
    val syntax = resolveSyntax(language)
    val n = code.length
    var i = 0

    fun addToken(style: SpanStyle, from: Int, to: Int) {
        if (to > from) addStyle(style, startOffset + from, startOffset + to)
    }

    while (i < n) {
        val c = code[i]

        val blockComment = syntax.blockComments.firstOrNull { code.startsWith(it.first, i) }
        if (blockComment != null) {
            val end = code.indexOf(blockComment.second, i + blockComment.first.length)
            val tokenEnd = if (end == -1) n else end + blockComment.second.length
            addToken(SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic), i, tokenEnd)
            i = tokenEnd
            continue
        }

        val lineComment = syntax.lineComments.firstOrNull { code.startsWith(it, i) }
        if (lineComment != null) {
            val end = code.indexOf('\n', i).let { if (it == -1) n else it }
            addToken(SpanStyle(color = colors.comment, fontStyle = FontStyle.Italic), i, end)
            i = end
            continue
        }

        if (c == '"' || c == '\'' || c == '`') {
            var j = i + 1
            while (j < n && code[j] != c) {
                if (code[j] == '\\' && j + 1 < n) j++
                j++
            }
            val end = (j + 1).coerceAtMost(n)
            addToken(SpanStyle(color = colors.string), i, end)
            i = end
            continue
        }

        if (c.isDigit() && (i == 0 || !isIdentifierPart(code[i - 1]))) {
            var j: Int
            if (c == '0' && i + 1 < n && (code[i + 1] == 'x' || code[i + 1] == 'X')) {
                j = i + 2
                while (j < n && (code[j].isDigit() || code[j] in 'a'..'f' || code[j] in 'A'..'F')) j++
            } else {
                j = i + 1
                while (j < n && code[j].isDigit()) j++
                if (j < n && code[j] == '.' && j + 1 < n && code[j + 1].isDigit()) {
                    j++
                    while (j < n && code[j].isDigit()) j++
                }
            }
            if (j < n && isIdentifierPart(code[j])) {
                // Not actually a numeric literal (e.g. the "3" in "3px") — skip past the whole
                // identifier-ish run unstyled instead of highlighting just its numeric prefix.
                while (j < n && isIdentifierPart(code[j])) j++
            } else {
                addToken(SpanStyle(color = colors.number), i, j)
            }
            i = j
            continue
        }

        if (isIdentifierStart(c)) {
            var j = i + 1
            while (j < n && isIdentifierPart(code[j])) j++
            val word = code.substring(i, j)
            if (word in syntax.keywords) {
                addToken(SpanStyle(color = colors.keyword, fontWeight = FontWeight.SemiBold), i, j)
            }
            i = j
            continue
        }

        i++
    }
}
