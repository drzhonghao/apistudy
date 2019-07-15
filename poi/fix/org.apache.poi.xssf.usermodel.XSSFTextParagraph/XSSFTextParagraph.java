

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.model.ParagraphPropertyFetcher;
import org.apache.poi.xssf.usermodel.ListAutoNumber;
import org.apache.poi.xssf.usermodel.TextAlign;
import org.apache.poi.xssf.usermodel.TextFontAlign;
import org.apache.poi.xssf.usermodel.XSSFTextRun;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextAutonumberBullet;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBodyProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBulletSizePercent;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBulletSizePoint;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharBullet;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextField;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextFont;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextLineBreak;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextNoBullet;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextNormalAutofit;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextSpacing;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextSpacingPercent;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextSpacingPoint;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextTabStop;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextTabStopList;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextAlignType;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextAutonumberScheme;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextFontAlignType;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTShape;

import static org.openxmlformats.schemas.drawingml.x2006.main.CTTextSpacing.Factory.newInstance;


public class XSSFTextParagraph implements Iterable<XSSFTextRun> {
	private final CTTextParagraph _p;

	private final CTShape _shape;

	private final List<XSSFTextRun> _runs;

	XSSFTextParagraph(CTTextParagraph p, CTShape ctShape) {
		_p = p;
		_shape = ctShape;
		_runs = new ArrayList<>();
		for (XmlObject ch : _p.selectPath("*")) {
			if (ch instanceof CTRegularTextRun) {
				CTRegularTextRun r = ((CTRegularTextRun) (ch));
			}else
				if (ch instanceof CTTextLineBreak) {
					CTTextLineBreak br = ((CTTextLineBreak) (ch));
					CTRegularTextRun r = CTRegularTextRun.Factory.newInstance();
					r.setRPr(br.getRPr());
					r.setT("\n");
				}else
					if (ch instanceof CTTextField) {
						CTTextField f = ((CTTextField) (ch));
						CTRegularTextRun r = CTRegularTextRun.Factory.newInstance();
						r.setRPr(f.getRPr());
						r.setT(f.getT());
					}


		}
	}

	public String getText() {
		StringBuilder out = new StringBuilder();
		for (XSSFTextRun r : _runs) {
			out.append(r.getText());
		}
		return out.toString();
	}

	@org.apache.poi.util.Internal
	public CTTextParagraph getXmlObject() {
		return _p;
	}

	@org.apache.poi.util.Internal
	public CTShape getParentShape() {
		return _shape;
	}

	public List<XSSFTextRun> getTextRuns() {
		return _runs;
	}

	public Iterator<XSSFTextRun> iterator() {
		return _runs.iterator();
	}

	public XSSFTextRun addNewTextRun() {
		CTRegularTextRun r = _p.addNewR();
		CTTextCharacterProperties rPr = r.addNewRPr();
		rPr.setLang("en-US");
		return null;
	}

	public XSSFTextRun addLineBreak() {
		CTTextLineBreak br = _p.addNewBr();
		CTTextCharacterProperties brProps = br.addNewRPr();
		if ((_runs.size()) > 0) {
		}
		CTRegularTextRun r = CTRegularTextRun.Factory.newInstance();
		r.setRPr(brProps);
		r.setT("\n");
		return null;
	}

	public TextAlign getTextAlign() {
		ParagraphPropertyFetcher<TextAlign> fetcher = new ParagraphPropertyFetcher<TextAlign>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetAlgn()) {
					TextAlign val = TextAlign.values()[((props.getAlgn().intValue()) - 1)];
					setValue(val);
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? TextAlign.LEFT : fetcher.getValue();
	}

	public void setTextAlign(TextAlign align) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		if (align == null) {
			if (pr.isSetAlgn())
				pr.unsetAlgn();

		}else {
			pr.setAlgn(STTextAlignType.Enum.forInt(((align.ordinal()) + 1)));
		}
	}

	public TextFontAlign getTextFontAlign() {
		ParagraphPropertyFetcher<TextFontAlign> fetcher = new ParagraphPropertyFetcher<TextFontAlign>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetFontAlgn()) {
					TextFontAlign val = TextFontAlign.values()[((props.getFontAlgn().intValue()) - 1)];
					setValue(val);
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? TextFontAlign.BASELINE : fetcher.getValue();
	}

	public void setTextFontAlign(TextFontAlign align) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		if (align == null) {
			if (pr.isSetFontAlgn())
				pr.unsetFontAlgn();

		}else {
			pr.setFontAlgn(STTextFontAlignType.Enum.forInt(((align.ordinal()) + 1)));
		}
	}

	public String getBulletFont() {
		ParagraphPropertyFetcher<String> fetcher = new ParagraphPropertyFetcher<String>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetBuFont()) {
					setValue(props.getBuFont().getTypeface());
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return fetcher.getValue();
	}

	public void setBulletFont(String typeface) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		CTTextFont font = (pr.isSetBuFont()) ? pr.getBuFont() : pr.addNewBuFont();
		font.setTypeface(typeface);
	}

	public String getBulletCharacter() {
		ParagraphPropertyFetcher<String> fetcher = new ParagraphPropertyFetcher<String>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetBuChar()) {
					setValue(props.getBuChar().getChar());
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return fetcher.getValue();
	}

	public void setBulletCharacter(String str) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		CTTextCharBullet c = (pr.isSetBuChar()) ? pr.getBuChar() : pr.addNewBuChar();
		c.setChar(str);
	}

	public Color getBulletFontColor() {
		ParagraphPropertyFetcher<Color> fetcher = new ParagraphPropertyFetcher<Color>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetBuClr()) {
					if (props.getBuClr().isSetSrgbClr()) {
						CTSRgbColor clr = props.getBuClr().getSrgbClr();
						byte[] rgb = clr.getVal();
						setValue(new Color((255 & (rgb[0])), (255 & (rgb[1])), (255 & (rgb[2]))));
						return true;
					}
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return fetcher.getValue();
	}

	public void setBulletFontColor(Color color) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		CTColor c = (pr.isSetBuClr()) ? pr.getBuClr() : pr.addNewBuClr();
		CTSRgbColor clr = (c.isSetSrgbClr()) ? c.getSrgbClr() : c.addNewSrgbClr();
		clr.setVal(new byte[]{ ((byte) (color.getRed())), ((byte) (color.getGreen())), ((byte) (color.getBlue())) });
	}

	public double getBulletFontSize() {
		ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetBuSzPct()) {
					setValue(((props.getBuSzPct().getVal()) * 0.001));
					return true;
				}
				if (props.isSetBuSzPts()) {
					setValue(((-(props.getBuSzPts().getVal())) * 0.01));
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? 100 : fetcher.getValue();
	}

	public void setBulletFontSize(double bulletSize) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		if (bulletSize >= 0) {
			CTTextBulletSizePercent pt = (pr.isSetBuSzPct()) ? pr.getBuSzPct() : pr.addNewBuSzPct();
			pt.setVal(((int) (bulletSize * 1000)));
			if (pr.isSetBuSzPts())
				pr.unsetBuSzPts();

		}else {
			CTTextBulletSizePoint pt = (pr.isSetBuSzPts()) ? pr.getBuSzPts() : pr.addNewBuSzPts();
			pt.setVal(((int) ((-bulletSize) * 100)));
			if (pr.isSetBuSzPct())
				pr.unsetBuSzPct();

		}
	}

	public void setIndent(double value) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		if (value == (-1)) {
			if (pr.isSetIndent())
				pr.unsetIndent();

		}else {
			pr.setIndent(Units.toEMU(value));
		}
	}

	public double getIndent() {
		ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetIndent()) {
					setValue(Units.toPoints(props.getIndent()));
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? 0 : fetcher.getValue();
	}

	public void setLeftMargin(double value) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		if (value == (-1)) {
			if (pr.isSetMarL())
				pr.unsetMarL();

		}else {
			pr.setMarL(Units.toEMU(value));
		}
	}

	public double getLeftMargin() {
		ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetMarL()) {
					double val = Units.toPoints(props.getMarL());
					setValue(val);
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? 0 : fetcher.getValue();
	}

	public void setRightMargin(double value) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		if (value == (-1)) {
			if (pr.isSetMarR())
				pr.unsetMarR();

		}else {
			pr.setMarR(Units.toEMU(value));
		}
	}

	public double getRightMargin() {
		ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetMarR()) {
					double val = Units.toPoints(props.getMarR());
					setValue(val);
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? 0 : fetcher.getValue();
	}

	public double getDefaultTabSize() {
		ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetDefTabSz()) {
					double val = Units.toPoints(props.getDefTabSz());
					setValue(val);
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? 0 : fetcher.getValue();
	}

	public double getTabStop(final int idx) {
		ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetTabLst()) {
					CTTextTabStopList tabStops = props.getTabLst();
					if (idx < (tabStops.sizeOfTabArray())) {
						CTTextTabStop ts = tabStops.getTabArray(idx);
						double val = Units.toPoints(ts.getPos());
						setValue(val);
						return true;
					}
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? 0.0 : fetcher.getValue();
	}

	public void addTabStop(double value) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		CTTextTabStopList tabStops = (pr.isSetTabLst()) ? pr.getTabLst() : pr.addNewTabLst();
		tabStops.addNewTab().setPos(Units.toEMU(value));
	}

	public void setLineSpacing(double linespacing) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		CTTextSpacing spc = newInstance();
		if (linespacing >= 0)
			spc.addNewSpcPct().setVal(((int) (linespacing * 1000)));
		else
			spc.addNewSpcPts().setVal(((int) ((-linespacing) * 100)));

		pr.setLnSpc(spc);
	}

	public double getLineSpacing() {
		ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetLnSpc()) {
					CTTextSpacing spc = props.getLnSpc();
					if (spc.isSetSpcPct())
						setValue(((spc.getSpcPct().getVal()) * 0.001));
					else
						if (spc.isSetSpcPts())
							setValue(((-(spc.getSpcPts().getVal())) * 0.01));


					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		double lnSpc = ((fetcher.getValue()) == null) ? 100 : fetcher.getValue();
		if (lnSpc > 0) {
			CTTextNormalAutofit normAutofit = _shape.getTxBody().getBodyPr().getNormAutofit();
			if (normAutofit != null) {
				double scale = 1 - (((double) (normAutofit.getLnSpcReduction())) / 100000);
				lnSpc *= scale;
			}
		}
		return lnSpc;
	}

	public void setSpaceBefore(double spaceBefore) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		CTTextSpacing spc = newInstance();
		if (spaceBefore >= 0)
			spc.addNewSpcPct().setVal(((int) (spaceBefore * 1000)));
		else
			spc.addNewSpcPts().setVal(((int) ((-spaceBefore) * 100)));

		pr.setSpcBef(spc);
	}

	public double getSpaceBefore() {
		ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetSpcBef()) {
					CTTextSpacing spc = props.getSpcBef();
					if (spc.isSetSpcPct())
						setValue(((spc.getSpcPct().getVal()) * 0.001));
					else
						if (spc.isSetSpcPts())
							setValue(((-(spc.getSpcPts().getVal())) * 0.01));


					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? 0 : fetcher.getValue();
	}

	public void setSpaceAfter(double spaceAfter) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		CTTextSpacing spc = newInstance();
		if (spaceAfter >= 0)
			spc.addNewSpcPct().setVal(((int) (spaceAfter * 1000)));
		else
			spc.addNewSpcPts().setVal(((int) ((-spaceAfter) * 100)));

		pr.setSpcAft(spc);
	}

	public double getSpaceAfter() {
		ParagraphPropertyFetcher<Double> fetcher = new ParagraphPropertyFetcher<Double>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetSpcAft()) {
					CTTextSpacing spc = props.getSpcAft();
					if (spc.isSetSpcPct())
						setValue(((spc.getSpcPct().getVal()) * 0.001));
					else
						if (spc.isSetSpcPts())
							setValue(((-(spc.getSpcPts().getVal())) * 0.01));


					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? 0 : fetcher.getValue();
	}

	public void setLevel(int level) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		pr.setLvl(level);
	}

	public int getLevel() {
		CTTextParagraphProperties pr = _p.getPPr();
		if (pr == null)
			return 0;

		return pr.getLvl();
	}

	public boolean isBullet() {
		ParagraphPropertyFetcher<Boolean> fetcher = new ParagraphPropertyFetcher<Boolean>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetBuNone()) {
					setValue(false);
					return true;
				}
				if (props.isSetBuFont()) {
					if ((props.isSetBuChar()) || (props.isSetBuAutoNum())) {
						setValue(true);
						return true;
					}
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? false : fetcher.getValue();
	}

	public void setBullet(boolean flag) {
		if ((isBullet()) == flag)
			return;

		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		if (!flag) {
			pr.addNewBuNone();
			if (pr.isSetBuAutoNum())
				pr.unsetBuAutoNum();

			if (pr.isSetBuBlip())
				pr.unsetBuBlip();

			if (pr.isSetBuChar())
				pr.unsetBuChar();

			if (pr.isSetBuClr())
				pr.unsetBuClr();

			if (pr.isSetBuClrTx())
				pr.unsetBuClrTx();

			if (pr.isSetBuFont())
				pr.unsetBuFont();

			if (pr.isSetBuFontTx())
				pr.unsetBuFontTx();

			if (pr.isSetBuSzPct())
				pr.unsetBuSzPct();

			if (pr.isSetBuSzPts())
				pr.unsetBuSzPts();

			if (pr.isSetBuSzTx())
				pr.unsetBuSzTx();

		}else {
			if (pr.isSetBuNone())
				pr.unsetBuNone();

			if (!(pr.isSetBuFont()))
				pr.addNewBuFont().setTypeface("Arial");

			if (!(pr.isSetBuAutoNum()))
				pr.addNewBuChar().setChar("\u2022");

		}
	}

	public void setBullet(ListAutoNumber scheme, int startAt) {
		if (startAt < 1)
			throw new IllegalArgumentException("Start Number must be greater or equal that 1");

		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		CTTextAutonumberBullet lst = (pr.isSetBuAutoNum()) ? pr.getBuAutoNum() : pr.addNewBuAutoNum();
		lst.setType(STTextAutonumberScheme.Enum.forInt(((scheme.ordinal()) + 1)));
		lst.setStartAt(startAt);
		if (!(pr.isSetBuFont()))
			pr.addNewBuFont().setTypeface("Arial");

		if (pr.isSetBuNone())
			pr.unsetBuNone();

		if (pr.isSetBuBlip())
			pr.unsetBuBlip();

		if (pr.isSetBuChar())
			pr.unsetBuChar();

	}

	public void setBullet(ListAutoNumber scheme) {
		CTTextParagraphProperties pr = (_p.isSetPPr()) ? _p.getPPr() : _p.addNewPPr();
		CTTextAutonumberBullet lst = (pr.isSetBuAutoNum()) ? pr.getBuAutoNum() : pr.addNewBuAutoNum();
		lst.setType(STTextAutonumberScheme.Enum.forInt(((scheme.ordinal()) + 1)));
		if (!(pr.isSetBuFont()))
			pr.addNewBuFont().setTypeface("Arial");

		if (pr.isSetBuNone())
			pr.unsetBuNone();

		if (pr.isSetBuBlip())
			pr.unsetBuBlip();

		if (pr.isSetBuChar())
			pr.unsetBuChar();

	}

	public boolean isBulletAutoNumber() {
		ParagraphPropertyFetcher<Boolean> fetcher = new ParagraphPropertyFetcher<Boolean>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetBuAutoNum()) {
					setValue(true);
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? false : fetcher.getValue();
	}

	public int getBulletAutoNumberStart() {
		ParagraphPropertyFetcher<Integer> fetcher = new ParagraphPropertyFetcher<Integer>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if ((props.isSetBuAutoNum()) && (props.getBuAutoNum().isSetStartAt())) {
					setValue(props.getBuAutoNum().getStartAt());
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? 0 : fetcher.getValue();
	}

	public ListAutoNumber getBulletAutoNumberScheme() {
		ParagraphPropertyFetcher<ListAutoNumber> fetcher = new ParagraphPropertyFetcher<ListAutoNumber>(getLevel()) {
			public boolean fetch(CTTextParagraphProperties props) {
				if (props.isSetBuAutoNum()) {
					setValue(ListAutoNumber.values()[((props.getBuAutoNum().getType().intValue()) - 1)]);
					return true;
				}
				return false;
			}
		};
		fetchParagraphProperty(fetcher);
		return (fetcher.getValue()) == null ? ListAutoNumber.ARABIC_PLAIN : fetcher.getValue();
	}

	@SuppressWarnings("rawtypes")
	private boolean fetchParagraphProperty(ParagraphPropertyFetcher visitor) {
		boolean ok = false;
		if (_p.isSetPPr())
			ok = visitor.fetch(_p.getPPr());

		if (!ok) {
			ok = visitor.fetch(_shape);
		}
		return ok;
	}

	@Override
	public String toString() {
		return (("[" + (getClass())) + "]") + (getText());
	}
}

