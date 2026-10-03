package io.github.alexxfromgit.taf.contract.core.report;

import java.util.List;

/** Minimal, dependency-free HTML builder for report attachments. All cell text is escaped. */
public final class Html {

    private static final String STYLE = """
            body{font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;margin:16px;color:#1f2328}
            h1{font-size:20px}h2{font-size:16px;margin-top:24px}
            table{border-collapse:collapse;width:100%;font-size:13px}
            th,td{border:1px solid #d0d7de;padding:6px 8px;text-align:left;vertical-align:top}
            th{background:#f6f8fa}td.num{text-align:right;font-variant-numeric:tabular-nums}
            .bad{background:#ffebe9}.good{background:#dafbe1}.muted{color:#656d76}
            code{font-family:ui-monospace,Consolas,monospace}
            """;

    private final StringBuilder body = new StringBuilder();
    private final String title;

    public Html(String title) {
        this.title = title;
        heading(1, title);
    }

    public Html heading(int level, String text) {
        body.append("<h").append(level).append('>').append(escape(text)).append("</h").append(level).append(">\n");
        return this;
    }

    public Html paragraph(String text) {
        body.append("<p>").append(escape(text)).append("</p>\n");
        return this;
    }

    /** Rows are lists of cells; a cell starting with {@code "!bad:"} or {@code "!good:"} is highlighted. */
    public Html table(List<String> headers, List<List<String>> rows) {
        body.append("<table><thead><tr>");
        headers.forEach(h -> body.append("<th>").append(escape(h)).append("</th>"));
        body.append("</tr></thead><tbody>\n");
        for (List<String> row : rows) {
            body.append("<tr>");
            for (String cell : row) {
                String css = "";
                String text = cell == null ? "" : cell;
                if (text.startsWith("!bad:")) {
                    css = "bad";
                    text = text.substring(5);
                } else if (text.startsWith("!good:")) {
                    css = "good";
                    text = text.substring(6);
                }
                if (text.matches("-?[0-9.,%]+")) {
                    css = (css + " num").trim();
                }
                body.append(css.isEmpty() ? "<td>" : "<td class=\"" + css + "\">")
                        .append(escape(text)).append("</td>");
            }
            body.append("</tr>\n");
        }
        body.append("</tbody></table>\n");
        return this;
    }

    public String render() {
        return "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + escape(title)
                + "</title><style>" + STYLE + "</style></head><body>\n" + body + "</body></html>\n";
    }

    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
