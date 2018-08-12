package tcl.lang;

import java.util.ArrayList;
import java.util.List;

public class RelocatedParser extends Parser {

    public static List<TclParse> parseCommand(String script, boolean nested) {
        List<TclParse> parses = new ArrayList<>();
		CharPointer src = new CharPointer(script);
		int len = script.length();
		int parseError = Parser.TCL_PARSE_SUCCESS;

		do {
			TclParse parse = parseCommand(null, src.array, src.index, len, null, 0, nested);
			parseError = parse.errorType;
			src.index = parse.commandStart + parse.commandSize;
			parse.release(); // Release parser resources
			if (src.index >= len) {
				break;
			}
			parses.add(parse);
		} while (parseError == Parser.TCL_PARSE_SUCCESS);

		return parses;
    }
}
