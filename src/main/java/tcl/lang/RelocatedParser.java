package tcl.lang;

public class RelocatedParser extends Parser {

    public static TclParse parseCommand(String script, boolean nested) {
        TclParse parse = null;
		CharPointer src = new CharPointer(script);
		int len = script.length();
		int parseError = Parser.TCL_PARSE_SUCCESS;

		do {
			parse = parseCommand(null, src.array, src.index, len, null, 0, nested);
			parseError = parse.errorType;
			src.index = parse.commandStart + parse.commandSize;
			parse.release(); // Release parser resources
			if (src.index >= len) {
				break;
			}
		} while (parseError == Parser.TCL_PARSE_SUCCESS);

		return parse;
    }
}
