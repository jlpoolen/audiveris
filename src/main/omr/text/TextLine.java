//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t L i n e                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.glyph.facets.Glyph;

import omr.score.entity.OldSystemPart;
import omr.score.entity.PartNode;

import omr.sheet.Skew;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code TextLine} defines a non-mutable structure to report all information on
 * one OCR-decoded line.
 *
 * @author Hervé Bitteur
 */
public class TextLine
        extends TextBasedItem
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TextLine.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Words that compose this line. */
    private final List<TextWord> words = new ArrayList<TextWord>();

    /** Average font for the line. */
    private FontInfo meanFont;

    /** Role of this text line. */
    private TextRoleInfo roleInfo;

    /** Temporary processed flag. */
    private boolean processed;

    //~ Constructors -------------------------------------------------------------------------------
    //
    //----------//
    // TextLine //
    //----------//
    /**
     * Creates a new TextLine object from a sequence of words.
     *
     * @param words the sequence of words
     */
    public TextLine (List<TextWord> words)
    {
        this();

        this.words.addAll(words);

        for (TextWord word : words) {
            word.setTextLine(this);
        }
    }

    //----------//
    // TextLine //
    //----------//
    /**
     * Creates a new TextLine object, without its contained words which are assumed
     * to be added later.
     */
    public TextLine ()
    {
        super(null, null, null, null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addWords //
    //----------//
    /**
     * Add a few words.
     *
     * @param words the words to add
     */
    public void addWords (Collection<TextWord> words)
    {
        if ((words != null) && !words.isEmpty()) {
            this.words.addAll(words);

            for (TextWord word : words) {
                word.setTextLine(this);
            }

            Collections.sort(this.words, TextWord.byAbscissa);

            invalidateCache();
        }
    }

    //
    //------------//
    // appendWord //
    //------------//
    /**
     * Append a word at the end of the word sequence of the line.
     *
     * @param word the word to append
     */
    public void appendWord (TextWord word)
    {
        words.add(word);
        word.setTextLine(this);
        invalidateCache();
    }

    /**
     * Give a Line comparator by de-skewed abscissa.
     *
     * @param skew the global sheet skew
     * @return the skew-based abscissa comparator
     */
    public static Comparator<TextLine> byAbscissa (final Skew skew)
    {
        return new Comparator<TextLine>()
        {
            @Override
            public int compare (TextLine line1,
                                TextLine line2)
            {
                return Double.compare(
                        line1.getDskOrigin(skew).getX(),
                        line2.getDskOrigin(skew).getX());
            }
        };
    }

    /**
     * Give a Line comparator by de-skewed ordinate.
     *
     * @param skew the global sheet skew
     * @return the skew-based ordinate comparator
     */
    public static Comparator<TextLine> byOrdinate (final Skew skew)
    {
        return new Comparator<TextLine>()
        {
            @Override
            public int compare (TextLine line1,
                                TextLine line2)
            {
                return Double.compare(
                        line1.getDskOrigin(skew).getY(),
                        line2.getDskOrigin(skew).getY());
            }
        };
    }

    //------//
    // dump //
    //------//
    /**
     * Print out internals.
     */
    public void dump ()
    {
        logger.info("{}", this);

        for (TextWord word : words) {
            logger.info("   {}", word);
        }
    }

    //-------------//
    // getBaseline //
    //-------------//
    /**
     * Overridden to recompute baseline from contained words
     *
     * @return the line baseline
     */
    @Override
    public Line2D getBaseline ()
    {
        if (super.getBaseline() == null) {
            if (words.isEmpty()) {
                return null;
            } else {
                setBaseline(baselineOf(words));
            }
        }

        return super.getBaseline();
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Overridden to recompute the bounds from contained words.
     *
     * @return the line bounds
     */
    @Override
    public Rectangle getBounds ()
    {
        if (super.getBounds() == null) {
            setBounds(boundsOf(getWords()));
        }

        return super.getBounds();
    }

    //----------//
    // getChars //
    //----------//
    /**
     * Report the sequence of chars descriptors (of words).
     *
     * @return the chars
     */
    public List<TextChar> getChars ()
    {
        List<TextChar> chars = new ArrayList<TextChar>();

        for (TextWord word : words) {
            chars.addAll(word.getChars());
        }

        return chars;
    }

    //---------------//
    // getConfidence //
    //---------------//
    /**
     * Overridden to recompute the confidence from contained words.
     *
     * @return the line confidence
     */
    @Override
    public Double getConfidence ()
    {
        if (super.getConfidence() == null) {
            setConfidence(confidenceOf(getWords()));
        }

        return super.getConfidence();
    }

    //--------------//
    // getDskOrigin //
    //--------------//
    /**
     * Report the de-skewed origin of this text line
     *
     * @param skew the sheet global skew
     * @return the de-skewed origin
     */
    public Point2D getDskOrigin (Skew skew)
    {
        Line2D base = getBaseline();

        if (base != null) {
            return skew.deskewed(base.getP1());
        }

        return null;
    }

    //--------------//
    // getFirstWord //
    //--------------//
    /**
     * Report the first word of the sentence.
     *
     * @return the first word
     */
    public TextWord getFirstWord ()
    {
        if (!words.isEmpty()) {
            return words.get(0);
        } else {
            return null;
        }
    }

    //-------------//
    // getMeanFont //
    //-------------//
    /**
     * Build a mean font (size, bold, serif) on representative words.
     *
     * @return the most representative font, or null if not available
     */
    public FontInfo getMeanFont ()
    {
        if (meanFont == null) {
            int charCount = 0; // Number of (representative) characters
            int boldCount = 0; // Number of rep chars with bold attribute
            int italicCount = 0; // Number of rep chars with italic attribute
            int serifCount = 0; // Number of rep chars with serif attribute
            int monospaceCount = 0; // Number of rep chars with monospace attribute
            int smallcapsCount = 0; // Number of rep chars with smallcaps attribute
            int underlinedCount = 0; // Number of rep chars with underlined attribute
            float sizeTotal = 0; // Total of font sizes on rep chars

            for (TextWord word : words) {
                int length = word.getLength();

                // Discard one-char words, they are not reliable
                if (length > 1) {
                    charCount += length;
                    sizeTotal += (word.getPreciseFontSize() * length);

                    FontInfo info = word.getFontInfo();

                    if (info.isBold) {
                        boldCount += length;
                    }

                    if (info.isItalic) {
                        italicCount += length;
                    }

                    if (info.isUnderlined) {
                        underlinedCount += length;
                    }

                    if (info.isMonospace) {
                        monospaceCount += length;
                    }

                    if (info.isSerif) {
                        serifCount += word.getLength();
                    }

                    if (info.isSmallcaps) {
                        smallcapsCount += length;
                    }
                }
            }

            if (charCount > 0) {
                int quorum = charCount / 2;
                meanFont = new FontInfo(
                        boldCount >= quorum, // isBold,
                        italicCount >= quorum, // isItalic,
                        underlinedCount >= quorum, // isUnderlined,
                        monospaceCount >= quorum, // isMonospace,
                        serifCount >= quorum, // isSerif,
                        smallcapsCount >= quorum, // isSmallcaps,
                        (int) Math.rint((double) sizeTotal / charCount),
                        "DummyFont");
            } else {
                // We have no representative data, let's use the first word
                if (getFirstWord() != null) {
                    meanFont = getFirstWord().getFontInfo();
                } else {
                    logger.error("TextLine with no first word {}", this);
                }
            }
        }

        return meanFont;
    }

    //---------//
    // getRole //
    //---------//
    /**
     * Report the line role.
     *
     * @return the roleInfo
     */
    public TextRoleInfo getRole ()
    {
        return roleInfo;
    }

    //---------------//
    // getSystemPart //
    //---------------//
    /**
     * Report the containing system part.
     *
     * @return the containing system part
     */
    public OldSystemPart getSystemPart ()
    {
        throw new RuntimeException("getSystemPart. Not yet implemented");

        //        final TextRole role = getRole().role;
        //        final Point location = getFirstWord().getLocation();
        //        final Staff staff = system.getScoreSystem().getTextStaff(role, location);
        //
        //        return staff.getPart();
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Overridden to return the concatenation of word values.
     *
     * @return the value to be used
     */
    @Override
    public String getValue ()
    {
        StringBuilder sb = null;

        // Use each word value
        for (TextWord word : words) {
            String str = word.getValue();

            if (sb == null) {
                sb = new StringBuilder(str);
            } else {
                sb.append(" ").append(str);
            }
        }

        if (sb == null) {
            return "";
        } else {
            return sb.toString();
        }
    }

    //---------------//
    // getWordGlyphs //
    //---------------//
    /**
     * Report the sequence of glyphs (parallel to the sequence of words)
     *
     * @return the sequence of word glyphs
     */
    public List<Glyph> getWordGlyphs ()
    {
        List<Glyph> glyphs = new ArrayList<Glyph>(words.size());

        for (TextWord word : words) {
            Glyph glyph = word.getGlyph();

            if (glyph != null) {
                glyphs.add(glyph);
            } else {
                logger.warn("Word {} with no related glyph", word);
            }
        }

        return glyphs;
    }

    //----------//
    // getWords //
    //----------//
    /**
     * Report an <b>unmodifiable</b> view of the sequence of words.
     *
     * @return the words view
     */
    public List<TextWord> getWords ()
    {
        return Collections.unmodifiableList(words);
    }

    //-------------//
    // isChordName //
    //-------------//
    /**
     * Report whether this line has the ChordName role
     *
     * @return true for chord line
     */
    public boolean isChordName ()
    {
        return getRole().role == TextRole.ChordName;
    }

    //----------//
    // isLyrics //
    //----------//
    /**
     * Report whether this line is flagged as a Lyrics line
     *
     * @return true for lyrics line
     */
    public boolean isLyrics ()
    {
        return getRole().role == TextRole.Lyrics;
    }

    //-------------//
    // isProcessed //
    //-------------//
    public boolean isProcessed ()
    {
        return processed;
    }

    //-------------//
    // removeWords //
    //-------------//
    /**
     * Remove a few words
     *
     * @param words the words to remove
     */
    public void removeWords (Collection<TextWord> words)
    {
        if ((words != null) && !words.isEmpty()) {
            this.words.removeAll(words);
            invalidateCache();
        }
    }

    //----------------------//
    // setGlyphsTranslation //
    //----------------------//
    /**
     * Forward the informationto all the words that compose this line.
     *
     * @param entity the same score entity for all sentence items
     */
    public void setGlyphsTranslation (PartNode entity)
    {
        for (TextWord word : words) {
            Glyph glyph = word.getGlyph();

            if (glyph != null) {
                glyph.setTranslation(entity);
            }
        }
    }

    //--------------//
    // setProcessed //
    //--------------//
    public void setProcessed (boolean processed)
    {
        this.processed = processed;
    }

    //---------//
    // setRole //
    //---------//
    /**
     * Assign role information.
     *
     * @param roleInfo the roleInfo to set
     */
    public void setRole (TextRoleInfo roleInfo)
    {
        this.roleInfo = roleInfo;
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to the coordinates of words descriptors.
     *
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    @Override
    public void translate (int dx,
                           int dy)
    {
        // Translate line bounds and baseline
        super.translate(dx, dy);

        // Translate contained descriptors
        for (TextWord word : words) {
            word.translate(dx, dy);
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (roleInfo != null) {
            sb.append(" ").append(roleInfo);
        }

        return sb.toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    private void invalidateCache ()
    {
        setBounds(null);

        setBaseline(null);
        setConfidence(null);

        roleInfo = null;
        meanFont = null;
    }
}
