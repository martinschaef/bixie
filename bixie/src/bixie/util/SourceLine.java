/**
 * 
 */
package bixie.util;

import org.gravy.util.JavaSourceLocation;

/**
 * this class is only here to make JavaSourceLocation
 * visible to the eclipse plugin.
 * @author schaef
 *
 */
public class SourceLine {
	public SourceLine(JavaSourceLocation other) {
		this.FileName=other.FileName;
		this.StartLine=other.StartLine;
		this.StartCol = other.StartCol;
		this.EndLine=other.EndLine;
		this.EndCol = other.EndCol;
		this.comment = other.comment;
		this.isCloned = other.isCloned;
		this.isNoVerify = other.isNoVerify;
		this.inInfeasibleBlock = other.inInfeasibleBlock;
	}
	
	public String FileName = "";
	public int StartLine = -1;
	public int EndLine = -1;
	public int StartCol = -1;
	public int EndCol = -1;
	public String comment = "";
	public boolean isCloned = false;
	public boolean isNoVerify = false;
	public boolean inInfeasibleBlock = false;

	@Override
	public boolean equals(Object other) {
		if (other instanceof JavaSourceLocation) {
			JavaSourceLocation o = (JavaSourceLocation) other;
			return o.FileName.equals(FileName)
					&& o.StartLine == this.StartLine
					&& o.EndLine == this.EndLine
					&& o.StartCol == this.StartCol
					&& o.EndCol == this.EndCol;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.FileName.hashCode() * StartLine * EndLine * StartCol
				* EndCol;
	}
	
}
