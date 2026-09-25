package one.nxeu.thaumory.aspect;

import static one.nxeu.thaumory.Thaumory.id;

import java.util.List;
import one.nxeu.thaumory.api.aspect.Aspect;
import one.nxeu.thaumory.api.aspect.AspectRegistry;
import one.nxeu.thaumory.api.text.TextEffect;

/** The built-in aspects: 6 primals in three opposing pairs and 12 compounds (requirements §2). */
public final class ThaumoryAspects {
    // Primals, counter-clockwise around the hexagon.
    public static final Aspect IGNIS = Aspect.primal(id("ignis"), 0xC9483E, 0).withNameEffect(TextEffect.FLICKER);
    public static final Aspect AER = Aspect.primal(id("aer"), 0xC7A928, 60).withNameEffect(TextEffect.WAVE);
    public static final Aspect VITA = Aspect.primal(id("vita"), 0x4E9F55, 120).withNameEffect(TextEffect.PULSE);
    public static final Aspect AQUA = Aspect.primal(id("aqua"), 0x3479BE, 180).withNameEffect(TextEffect.WAVE);
    public static final Aspect TERRA = Aspect.primal(id("terra"), 0x8A6538, 240);
    public static final Aspect MORS = Aspect.primal(id("mors"), 0x6B5B8E, 300).withNameEffect(TextEffect.TREMBLE);

    // Adjacent primals (60° apart).
    public static final Aspect LUX = Aspect.compound(id("lux"), 0xF2E6A0, IGNIS, AER).withNameEffect(TextEffect.STREAK);
    public static final Aspect ARCANUM = Aspect.compound(id("arcanum"), 0x9B6FD6, AER, VITA).withNameEffect(TextEffect.SHIMMER);
    public static final Aspect HERBA = Aspect.compound(id("herba"), 0x3F8F3A, VITA, AQUA).withNameEffect(TextEffect.PULSE);
    public static final Aspect UMBRA = Aspect.compound(id("umbra"), 0x2B2D42, AQUA, TERRA).withNameEffect(TextEffect.PULSE);
    public static final Aspect VINCULUM = Aspect.compound(id("vinculum"), 0x5E4B3C, TERRA, MORS);
    public static final Aspect BELLUM = Aspect.compound(id("bellum"), 0x8E1F2A, MORS, IGNIS).withNameEffect(TextEffect.TREMBLE);

    // Primals one step apart (120°).
    public static final Aspect BESTIA = Aspect.compound(id("bestia"), 0xB5723B, IGNIS, VITA);
    public static final Aspect TEMPESTAS = Aspect.compound(id("tempestas"), 0x7FA7C9, AER, AQUA).withNameEffect(TextEffect.SHAKE);
    public static final Aspect ORDO = Aspect.compound(id("ordo"), 0xD9D4C7, VITA, TERRA);
    public static final Aspect VENENUM = Aspect.compound(id("venenum"), 0x6E8B2E, AQUA, MORS).withNameEffect(TextEffect.FLICKER);
    public static final Aspect METALLUM = Aspect.compound(id("metallum"), 0x8C959C, TERRA, IGNIS).withNameEffect(TextEffect.STREAK);
    public static final Aspect CHAOS = Aspect.compound(id("chaos"), 0x4A1F5C, MORS, AER).withNameEffect(TextEffect.SHAKE);

    // The third tier (requirements §2.2), in opposite pairs.
    public static final Aspect SORDES = Aspect.compound(id("sordes"), 0x5A3F66, VENENUM, VINCULUM).withNameEffect(TextEffect.TREMBLE);
    public static final Aspect ANIMA = Aspect.compound(id("anima"), 0xE8A0B0, ARCANUM, BESTIA).withNameEffect(TextEffect.PULSE);
    public static final Aspect AURORA = Aspect.compound(id("aurora"), 0xF0C080, LUX, ARCANUM).withNameEffect(TextEffect.STREAK);
    public static final Aspect VESPER = Aspect.compound(id("vesper"), 0x2E3A7A, UMBRA, VINCULUM).withNameEffect(TextEffect.SHIMMER);
    public static final Aspect ABYSSUS = Aspect.compound(id("abyssus"), 0x141830, UMBRA, MORS).withNameEffect(TextEffect.PULSE);
    public static final Aspect CAELUM = Aspect.compound(id("caelum"), 0x8CC8F0, LUX, VITA).withNameEffect(TextEffect.WAVE);
    public static final Aspect SIGILLUM = Aspect.compound(id("sigillum"), 0xA08A4A, VINCULUM, METALLUM);
    public static final Aspect SOLUTIO = Aspect.compound(id("solutio"), 0x8FD3C0, ARCANUM, TEMPESTAS).withNameEffect(TextEffect.WAVE);
    public static final Aspect TARTARUS = Aspect.compound(id("tartarus"), 0x7A1E14, BELLUM, IGNIS).withNameEffect(TextEffect.FLICKER);
    public static final Aspect FONS = Aspect.compound(id("fons"), 0x2FA898, HERBA, AQUA).withNameEffect(TextEffect.WAVE);
    public static final Aspect PEREGRINUM = Aspect.compound(id("peregrinum"), 0xB89AD8, CHAOS, TEMPESTAS).withNameEffect(TextEffect.SHAKE);
    public static final Aspect ARTIFICIUM = Aspect.compound(id("artificium"), 0xB5893A, ORDO, METALLUM);

    public static final List<Aspect> ALL = List.of(
            IGNIS, AER, VITA, AQUA, TERRA, MORS,
            LUX, ARCANUM, HERBA, UMBRA, VINCULUM, BELLUM,
            BESTIA, TEMPESTAS, ORDO, VENENUM, METALLUM, CHAOS,
            SORDES, ANIMA, AURORA, VESPER, ABYSSUS, CAELUM, SIGILLUM, SOLUTIO, TARTARUS, FONS, PEREGRINUM, ARTIFICIUM);

    private ThaumoryAspects() {}

    public static void register(AspectRegistry registry) {
        ALL.forEach(registry::register);
    }
}
