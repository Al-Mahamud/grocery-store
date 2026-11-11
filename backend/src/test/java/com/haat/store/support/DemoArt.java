package com.haat.store.support;

import java.util.Locale;
import java.util.Random;

/**
 * Draws grocery packshots as SVG, standing in for product photos in demo data: a 600 x 600 square
 * with the item on a soft tinted blob, like products photographed on white.
 */
public final class DemoArt {

    public enum Kind {
        ROUND, ONION, POTATO, CARROT, CUCUMBER, EGGPLANT, CHILLI, BANANA, LEAFY,
        BAG, BOTTLE, CARTON, JAR, BOX, EGGS, PACK, BREAD, FISH, MEAT, SOAP, CAN
    }

    /** {@code color} is the item, {@code accent} the label or leaves, {@code label} the text on packaged goods. */
    public record Item(Kind kind, String color, String accent, String label, String sub, long seed) {
    }

    private DemoArt() {
    }

    public static String draw(Item item) {
        Random r = new Random(item.seed());
        StringBuilder s = new StringBuilder("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 600 600\">");
        s.append("<defs>")
                .append("<radialGradient id=\"sheen\" cx=\"35%\" cy=\"30%\" r=\"70%\"><stop offset=\"0\" stop-color=\"#fff\" stop-opacity=\".55\"/>")
                .append("<stop offset=\".5\" stop-color=\"#fff\" stop-opacity=\"0\"/><stop offset=\"1\" stop-color=\"#000\" stop-opacity=\".18\"/></radialGradient>")
                .append("<linearGradient id=\"side\" x1=\"0\" x2=\"1\"><stop offset=\"0\" stop-color=\"#fff\" stop-opacity=\".25\"/>")
                .append("<stop offset=\".35\" stop-color=\"#fff\" stop-opacity=\"0\"/><stop offset=\"1\" stop-color=\"#000\" stop-opacity=\".16\"/></linearGradient>")
                .append("<filter id=\"shadow\" x=\"-30%\" y=\"-30%\" width=\"160%\" height=\"160%\"><feGaussianBlur stdDeviation=\"12\"/></filter>")
                .append("</defs>");
        s.append("<rect width=\"600\" height=\"600\" fill=\"#FFFFFF\"/>");
        s.append(blob(mix(item.color(), "#FFFFFF", .86), r));
        s.append("<ellipse cx=\"300\" cy=\"505\" rx=\"170\" ry=\"20\" fill=\"#1A1A2E\" opacity=\".14\" filter=\"url(#shadow)\"/>");
        s.append(switch (item.kind()) {
            case ROUND -> round(item, r);
            case ONION -> onion(item, r);
            case POTATO -> potato(item, r);
            case CARROT -> carrot(item);
            case CUCUMBER -> cucumber(item);
            case EGGPLANT -> eggplant(item);
            case CHILLI -> chilli(item, r);
            case BANANA -> banana(item);
            case LEAFY -> leafy(item, r);
            case BAG -> bag(item, r);
            case BOTTLE -> bottle(item);
            case CARTON -> carton(item);
            case JAR -> jar(item);
            case BOX -> box(item);
            case EGGS -> eggs(item);
            case PACK -> pack(item);
            case BREAD -> bread(item);
            case FISH -> fish(item);
            case MEAT -> meat(item, r);
            case SOAP -> soap(item);
            case CAN -> can(item);
        });
        return s.append("</svg>").toString();
    }

    /** A soft, slightly irregular round shape: quadratic curves through the midpoints of jittered points. */
    private static String blob(String color, Random r) {
        int n = 9;
        double[][] p = new double[n][2];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            double rad = 240 + r.nextDouble() * 30;
            p[i][0] = 300 + Math.cos(a) * rad;
            p[i][1] = 295 + Math.sin(a) * rad * .9;
        }
        StringBuilder d = new StringBuilder();
        for (int i = 0; i <= n; i++) {
            double[] a = p[i % n];
            double[] b = p[(i + 1) % n];
            double mx = (a[0] + b[0]) / 2;
            double my = (a[1] + b[1]) / 2;
            d.append(i == 0 ? "M" + f(mx) + " " + f(my) : " Q" + f(a[0]) + " " + f(a[1]) + " " + f(mx) + " " + f(my));
        }
        return "<path d=\"" + d + " Z\" fill=\"" + color + "\"/>";
    }

    // ------------------------------------------------------------------ produce

    private static String round(Item item, Random r) {
        double[][] spots = {{230, 340, 92}, {370, 345, 92}, {300, 250, 92}};
        StringBuilder s = new StringBuilder();
        for (double[] p : spots) {
            double x = p[0] + (r.nextDouble() - .5) * 10;
            double y = p[1] + (r.nextDouble() - .5) * 10;
            s.append(circle(x, y, p[2], item.color())).append(circle(x, y, p[2], "url(#sheen)"));
            s.append("<path d=\"M").append(f(x)).append(" ").append(f(y - p[2] + 8)).append(" l-16 -6 l10 10 l-12 8 l18 -2 l0 14 l6 -14 l16 4 l-10 -12 l12 -10 l-18 4 Z\" fill=\"")
                    .append(item.accent()).append("\"/>");
            s.append(circle(x - p[2] * .35, y - p[2] * .35, p[2] * .14, "#fff\" opacity=\".55"));
        }
        return s.toString();
    }

    private static String onion(Item item, Random r) {
        StringBuilder s = new StringBuilder();
        double[][] spots = {{225, 350}, {375, 350}, {300, 270}};
        for (double[] p : spots) {
            double x = p[0];
            double y = p[1];
            s.append("<path d=\"M").append(f(x)).append(" ").append(f(y - 120)).append(" C").append(f(x + 20)).append(" ").append(f(y - 80))
                    .append(" ").append(f(x + 95)).append(" ").append(f(y - 50)).append(" ").append(f(x + 90)).append(" ").append(f(y + 20))
                    .append(" C").append(f(x + 85)).append(" ").append(f(y + 85)).append(" ").append(f(x - 85)).append(" ").append(f(y + 85))
                    .append(" ").append(f(x - 90)).append(" ").append(f(y + 20)).append(" C").append(f(x - 95)).append(" ").append(f(y - 50))
                    .append(" ").append(f(x - 20)).append(" ").append(f(y - 80)).append(" ").append(f(x)).append(" ").append(f(y - 120))
                    .append(" Z\" fill=\"").append(item.color()).append("\"/>");
            for (int k = -2; k <= 2; k++) {
                s.append("<path d=\"M").append(f(x)).append(" ").append(f(y - 110)).append(" Q").append(f(x + k * 45)).append(" ")
                        .append(f(y - 20)).append(" ").append(f(x + k * 22)).append(" ").append(f(y + 78))
                        .append("\" stroke=\"").append(darken(item.color(), .2)).append("\" stroke-width=\"2.5\" fill=\"none\" opacity=\".5\"/>");
            }
            s.append("<path d=\"M").append(f(x - 10)).append(" ").append(f(y + 78)).append(" l-6 14 M").append(f(x)).append(" ").append(f(y + 80))
                    .append(" l0 16 M").append(f(x + 10)).append(" ").append(f(y + 78)).append(" l6 14\" stroke=\"#C9B38A\" stroke-width=\"3\"/>");
        }
        return s + "<rect width=\"600\" height=\"600\" fill=\"none\"/>";
    }

    private static String potato(Item item, Random r) {
        StringBuilder s = new StringBuilder();
        double[][] spots = {{210, 360, 100, 70, -10}, {380, 365, 95, 66, 15}, {300, 270, 105, 72, 5}};
        for (double[] p : spots) {
            s.append("<ellipse cx=\"").append(f(p[0])).append("\" cy=\"").append(f(p[1])).append("\" rx=\"").append(f(p[2]))
                    .append("\" ry=\"").append(f(p[3])).append("\" fill=\"").append(item.color()).append("\" transform=\"rotate(")
                    .append(f(p[4])).append(" ").append(f(p[0])).append(" ").append(f(p[1])).append(")\"/>");
            s.append("<ellipse cx=\"").append(f(p[0])).append("\" cy=\"").append(f(p[1])).append("\" rx=\"").append(f(p[2]))
                    .append("\" ry=\"").append(f(p[3])).append("\" fill=\"url(#sheen)\"/>");
            for (int k = 0; k < 6; k++) {
                s.append(circle(p[0] + (r.nextDouble() - .5) * p[2] * 1.4, p[1] + (r.nextDouble() - .5) * p[3] * 1.3, 3, darken(item.color(), .35)));
            }
        }
        return s.toString();
    }

    private static String carrot(Item item) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            double x = 220 + i * 80;
            double rot = -18 + i * 18;
            s.append("<g transform=\"rotate(").append(f(rot)).append(" ").append(f(x)).append(" 300)\">");
            for (int k = -1; k <= 1; k++) {
                s.append("<path d=\"M").append(f(x)).append(" 150 q").append(k * 30).append(" -60 ").append(k * 40).append(" -80\" stroke=\"")
                        .append(item.accent()).append("\" stroke-width=\"10\" stroke-linecap=\"round\" fill=\"none\"/>");
            }
            s.append("<path d=\"M").append(f(x - 38)).append(" 150 Q").append(f(x)).append(" 135 ").append(f(x + 38)).append(" 150 L")
                    .append(f(x + 6)).append(" 470 Q").append(f(x)).append(" 480 ").append(f(x - 6)).append(" 470 Z\" fill=\"").append(item.color()).append("\"/>");
            s.append("<path d=\"M").append(f(x - 38)).append(" 150 Q").append(f(x)).append(" 135 ").append(f(x + 38)).append(" 150 L")
                    .append(f(x + 6)).append(" 470 Q").append(f(x)).append(" 480 ").append(f(x - 6)).append(" 470 Z\" fill=\"url(#side)\"/>");
            for (int k = 0; k < 6; k++) {
                double y = 190 + k * 45;
                double w = 30 - k * 4;
                s.append("<path d=\"M").append(f(x - w)).append(" ").append(f(y)).append(" l").append(f(w * .6)).append(" 4\" stroke=\"")
                        .append(darken(item.color(), .25)).append("\" stroke-width=\"3\" stroke-linecap=\"round\"/>");
            }
            s.append("</g>");
        }
        return s.toString();
    }

    private static String cucumber(Item item) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            double y = 230 + i * 70;
            s.append("<g transform=\"rotate(").append(-20 + i * 6).append(" 300 ").append(f(y)).append(")\">");
            s.append("<rect x=\"110\" y=\"").append(f(y - 38)).append("\" width=\"380\" height=\"76\" rx=\"38\" fill=\"").append(item.color()).append("\"/>");
            s.append("<rect x=\"110\" y=\"").append(f(y - 38)).append("\" width=\"380\" height=\"76\" rx=\"38\" fill=\"url(#sheen)\"/>");
            for (int k = 0; k < 9; k++) {
                s.append(circle(150 + k * 38, y - 10 + (k % 2) * 20, 3, lighten(item.color(), .45)));
            }
            s.append("</g>");
        }
        return s.toString();
    }

    private static String eggplant(Item item) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 2; i++) {
            double x = 245 + i * 120;
            double rot = -15 + i * 30;
            s.append("<g transform=\"rotate(").append(f(rot)).append(" ").append(f(x)).append(" 320)\">");
            s.append("<path d=\"M").append(f(x)).append(" 170 C").append(f(x + 50)).append(" 180 ").append(f(x + 95)).append(" 300 ").append(f(x + 85)).append(" 400 ")
                    .append("C").append(f(x + 75)).append(" 490 ").append(f(x - 75)).append(" 490 ").append(f(x - 85)).append(" 400 ")
                    .append("C").append(f(x - 95)).append(" 300 ").append(f(x - 50)).append(" 180 ").append(f(x)).append(" 170 Z\" fill=\"").append(item.color()).append("\"/>");
            s.append("<path d=\"M").append(f(x)).append(" 170 C").append(f(x + 50)).append(" 180 ").append(f(x + 95)).append(" 300 ").append(f(x + 85)).append(" 400 ")
                    .append("C").append(f(x + 75)).append(" 490 ").append(f(x - 75)).append(" 490 ").append(f(x - 85)).append(" 400 ")
                    .append("C").append(f(x - 95)).append(" 300 ").append(f(x - 50)).append(" 180 ").append(f(x)).append(" 170 Z\" fill=\"url(#sheen)\"/>");
            s.append("<path d=\"M").append(f(x - 45)).append(" 200 Q").append(f(x)).append(" 150 ").append(f(x + 45)).append(" 200 L").append(f(x + 20))
                    .append(" 185 L").append(f(x)).append(" 215 L").append(f(x - 20)).append(" 185 Z\" fill=\"").append(item.accent()).append("\"/>");
            s.append("<rect x=\"").append(f(x - 7)).append("\" y=\"120\" width=\"14\" height=\"55\" rx=\"6\" fill=\"").append(item.accent()).append("\"/>");
            s.append("</g>");
        }
        return s.toString();
    }

    private static String chilli(Item item, Random r) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            double x = 170 + i * 42 + (r.nextDouble() - .5) * 20;
            double y = 200 + (i % 3) * 40;
            double bend = (r.nextDouble() - .3) * 70;
            s.append("<path d=\"M").append(f(x)).append(" ").append(f(y)).append(" Q").append(f(x + bend)).append(" ").append(f(y + 150))
                    .append(" ").append(f(x + bend * .4 + 10)).append(" ").append(f(y + 260)).append("\" stroke=\"").append(item.color())
                    .append("\" stroke-width=\"24\" stroke-linecap=\"round\" fill=\"none\"/>");
            s.append("<path d=\"M").append(f(x - 3)).append(" ").append(f(y + 10)).append(" Q").append(f(x + bend - 6)).append(" ").append(f(y + 150))
                    .append(" ").append(f(x + bend * .4 + 4)).append(" ").append(f(y + 240)).append("\" stroke=\"#fff\" stroke-width=\"5\" stroke-linecap=\"round\" fill=\"none\" opacity=\".35\"/>");
            s.append("<path d=\"M").append(f(x)).append(" ").append(f(y)).append(" q4 -30 20 -40\" stroke=\"").append(item.accent())
                    .append("\" stroke-width=\"7\" stroke-linecap=\"round\" fill=\"none\"/>");
        }
        return s.toString();
    }

    private static String banana(Item item) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            double dy = i * 26;
            String shade = i % 2 == 0 ? item.color() : darken(item.color(), .06);
            s.append("<path d=\"M150 ").append(f(220 + dy)).append(" Q300 ").append(f(470 + dy * .3)).append(" 470 ").append(f(250 + dy * .5))
                    .append(" Q460 ").append(f(290 + dy * .5)).append(" 450 ").append(f(300 + dy * .5)).append(" Q300 ").append(f(420 + dy * .3))
                    .append(" 165 ").append(f(205 + dy)).append(" Z\" fill=\"").append(shade).append("\" stroke=\"").append(darken(item.color(), .2))
                    .append("\" stroke-width=\"2\"/>");
        }
        s.append("<path d=\"M130 200 L170 225 L165 190 Z\" fill=\"").append(item.accent()).append("\"/>");
        s.append("<rect x=\"118\" y=\"180\" width=\"40\" height=\"30\" rx=\"8\" fill=\"").append(item.accent()).append("\"/>");
        return s.toString();
    }

    private static String leafy(Item item, Random r) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            double angle = -70 + i * 10 + (r.nextDouble() - .5) * 8;
            double len = 170 + r.nextDouble() * 50;
            String shade = i % 3 == 0 ? darken(item.color(), .12) : i % 3 == 1 ? item.color() : lighten(item.color(), .12);
            s.append("<g transform=\"rotate(").append(f(angle)).append(" 300 460)\">")
                    .append("<path d=\"M300 460 L300 ").append(f(460 - len * .45)).append("\" stroke=\"").append(lighten(item.color(), .3)).append("\" stroke-width=\"6\"/>")
                    .append("<ellipse cx=\"300\" cy=\"").append(f(460 - len * .7)).append("\" rx=\"34\" ry=\"").append(f(len * .32))
                    .append("\" fill=\"").append(shade).append("\"/>")
                    .append("<path d=\"M300 ").append(f(460 - len * .45)).append(" L300 ").append(f(460 - len * .98)).append("\" stroke=\"")
                    .append(lighten(item.color(), .35)).append("\" stroke-width=\"2\"/></g>");
        }
        s.append("<rect x=\"270\" y=\"400\" width=\"60\" height=\"26\" rx=\"6\" fill=\"").append(item.accent()).append("\"/>");
        return s.toString();
    }

    // ------------------------------------------------------------------ packaged

    private static String label(double cx, double cy, double w, String title, String sub, String color, double size) {
        StringBuilder s = new StringBuilder();
        s.append("<text x=\"").append(f(cx)).append("\" y=\"").append(f(cy)).append("\" text-anchor=\"middle\" font-family=\"Arial, Helvetica, sans-serif\" font-weight=\"800\" font-size=\"")
                .append(f(size)).append("\" fill=\"").append(color).append("\" textLength=\"").append(f(Math.min(w, title.length() * size * .62)))
                .append("\" lengthAdjust=\"spacingAndGlyphs\">").append(escape(title)).append("</text>");
        if (sub != null) {
            s.append("<text x=\"").append(f(cx)).append("\" y=\"").append(f(cy + size * 1.15)).append("\" text-anchor=\"middle\" font-family=\"Arial, Helvetica, sans-serif\" font-weight=\"700\" font-size=\"")
                    .append(f(size * .62)).append("\" fill=\"").append(color).append("\" opacity=\".8\">").append(escape(sub)).append("</text>");
        }
        return s.toString();
    }

    private static String brand(double cx, double cy, String color) {
        return "<text x=\"" + f(cx) + "\" y=\"" + f(cy) + "\" text-anchor=\"middle\" font-family=\"Arial, Helvetica, sans-serif\" font-weight=\"700\" "
                + "font-size=\"15\" letter-spacing=\"3\" fill=\"" + color + "\">HAAT SELECT</text>";
    }

    private static String bag(Item item, Random r) {
        String c = item.color();
        StringBuilder s = new StringBuilder();
        s.append("<path d=\"M175 120 L425 120 L432 150 Q450 300 438 470 Q300 490 162 470 Q150 300 168 150 Z\" fill=\"").append(c).append("\"/>");
        s.append("<path d=\"M175 120 L425 120 L432 150 Q450 300 438 470 Q300 490 162 470 Q150 300 168 150 Z\" fill=\"url(#side)\"/>");
        s.append("<path d=\"M175 120 L425 120 L425 145 L175 145 Z\" fill=\"").append(darken(c, .2)).append("\"/>");
        for (int x = 180; x < 425; x += 12) {
            s.append("<path d=\"M").append(x).append(" 120 l6 -8 l6 8\" fill=\"").append(darken(c, .2)).append("\"/>");
        }
        s.append("<rect x=\"200\" y=\"200\" width=\"200\" height=\"190\" rx=\"18\" fill=\"#FFFDF7\"/>");
        s.append(brand(300, 232, item.accent()));
        s.append(label(300, 290, 170, item.label(), item.sub(), "#1A1A2E", 34));
        for (int k = 0; k < 9; k++) {
            s.append("<ellipse cx=\"").append(f(250 + (k % 5) * 25 + (r.nextDouble() - .5) * 6)).append("\" cy=\"").append(f(355 + (k / 5) * 14))
                    .append("\" rx=\"7\" ry=\"3.5\" fill=\"").append(item.accent()).append("\" opacity=\".7\"/>");
        }
        return s.toString();
    }

    private static String bottle(Item item) {
        String c = item.color();
        return "<rect x=\"262\" y=\"70\" width=\"76\" height=\"44\" rx=\"8\" fill=\"" + item.accent() + "\"/>"
                + "<path d=\"M272 114 L328 114 L332 150 Q395 185 395 250 L395 455 Q395 485 365 485 L235 485 Q205 485 205 455 L205 250 Q205 185 268 150 Z\" fill=\"" + c + "\"/>"
                + "<path d=\"M272 114 L328 114 L332 150 Q395 185 395 250 L395 455 Q395 485 365 485 L235 485 Q205 485 205 455 L205 250 Q205 185 268 150 Z\" fill=\"url(#side)\"/>"
                + "<rect x=\"205\" y=\"280\" width=\"190\" height=\"140\" fill=\"#FFFDF7\"/>"
                + "<rect x=\"205\" y=\"280\" width=\"190\" height=\"14\" fill=\"" + item.accent() + "\"/>"
                + brand(300, 318, item.accent())
                + label(300, 362, 160, item.label(), item.sub(), "#1A1A2E", 28)
                + "<rect x=\"222\" y=\"190\" width=\"14\" height=\"80\" rx=\"7\" fill=\"#fff\" opacity=\".35\"/>";
    }

    private static String carton(Item item) {
        String c = item.color();
        return "<path d=\"M210 160 L300 100 L390 160 Z\" fill=\"" + darken(c, .08) + "\"/>"
                + "<rect x=\"290\" y=\"82\" width=\"20\" height=\"26\" rx=\"3\" fill=\"" + darken(c, .2) + "\"/>"
                + "<rect x=\"210\" y=\"160\" width=\"180\" height=\"320\" fill=\"" + c + "\"/>"
                + "<path d=\"M390 160 L430 140 L430 455 L390 480 Z\" fill=\"" + darken(c, .18) + "\"/>"
                + "<path d=\"M390 160 L300 100 L345 88 L430 140 Z\" fill=\"" + darken(c, .12) + "\"/>"
                + "<rect x=\"210\" y=\"250\" width=\"180\" height=\"150\" fill=\"#FFFDF7\"/>"
                + brand(300, 282, item.accent())
                + label(300, 330, 150, item.label(), item.sub(), "#1A1A2E", 30)
                + "<path d=\"M210 430 Q300 400 390 430 L390 480 L210 480 Z\" fill=\"" + item.accent() + "\"/>";
    }

    private static String jar(Item item) {
        String c = item.color();
        return "<rect x=\"215\" y=\"110\" width=\"170\" height=\"60\" rx=\"12\" fill=\"" + item.accent() + "\"/>"
                + "<rect x=\"215\" y=\"110\" width=\"170\" height=\"60\" rx=\"12\" fill=\"url(#side)\"/>"
                + "<path d=\"M205 170 L395 170 Q420 190 420 230 L420 450 Q420 485 385 485 L215 485 Q180 485 180 450 L180 230 Q180 190 205 170 Z\" fill=\"#E9EEF2\" opacity=\".9\"/>"
                + "<path d=\"M190 215 L410 215 L410 450 Q410 475 385 475 L215 475 Q190 475 190 450 Z\" fill=\"" + c + "\"/>"
                + "<path d=\"M180 230 L420 230 L420 450 Q420 485 385 485 L215 485 Q180 485 180 450 Z\" fill=\"url(#side)\"/>"
                + "<rect x=\"200\" y=\"280\" width=\"200\" height=\"120\" rx=\"10\" fill=\"#FFFDF7\"/>"
                + brand(300, 310, item.accent())
                + label(300, 352, 170, item.label(), item.sub(), "#1A1A2E", 28)
                + "<rect x=\"196\" y=\"200\" width=\"12\" height=\"230\" rx=\"6\" fill=\"#fff\" opacity=\".45\"/>";
    }

    private static String box(Item item) {
        String c = item.color();
        return "<path d=\"M180 160 L240 120 L450 120 L390 160 Z\" fill=\"" + lighten(c, .15) + "\"/>"
                + "<path d=\"M390 160 L450 120 L450 430 L390 470 Z\" fill=\"" + darken(c, .2) + "\"/>"
                + "<rect x=\"180\" y=\"160\" width=\"210\" height=\"310\" fill=\"" + c + "\"/>"
                + "<circle cx=\"285\" cy=\"260\" r=\"62\" fill=\"" + item.accent() + "\" opacity=\".9\"/>"
                + "<rect x=\"180\" y=\"340\" width=\"210\" height=\"100\" fill=\"#FFFDF7\"/>"
                + brand(285, 200, "#FFFDF7")
                + label(285, 383, 180, item.label(), item.sub(), "#1A1A2E", 28);
    }

    private static String eggs(Item item) {
        StringBuilder s = new StringBuilder();
        s.append("<path d=\"M110 300 L490 300 L465 450 L135 450 Z\" fill=\"#C9C2B2\"/>");
        s.append("<path d=\"M110 300 L490 300 L465 450 L135 450 Z\" fill=\"url(#side)\"/>");
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                double x = 175 + col * 83 + row * 12;
                double y = 255 + row * 55;
                s.append("<ellipse cx=\"").append(f(x)).append("\" cy=\"").append(f(y)).append("\" rx=\"38\" ry=\"48\" fill=\"").append(item.color()).append("\"/>");
                s.append("<ellipse cx=\"").append(f(x)).append("\" cy=\"").append(f(y)).append("\" rx=\"38\" ry=\"48\" fill=\"url(#sheen)\"/>");
            }
        }
        s.append("<rect x=\"135\" y=\"380\" width=\"330\" height=\"60\" rx=\"8\" fill=\"#FFFDF7\"/>");
        s.append(label(300, 418, 280, item.label(), null, "#1A1A2E", 26));
        return s.toString();
    }

    private static String pack(Item item) {
        String c = item.color();
        StringBuilder s = new StringBuilder();
        s.append("<path d=\"M140 170 Q300 140 460 170 Q480 300 460 430 Q300 460 140 430 Q120 300 140 170 Z\" fill=\"").append(c).append("\"/>");
        s.append("<path d=\"M140 170 Q300 140 460 170 Q480 300 460 430 Q300 460 140 430 Q120 300 140 170 Z\" fill=\"url(#sheen)\"/>");
        for (int x = 145; x < 460; x += 14) {
            s.append("<path d=\"M").append(x).append(" 168 l7 -14 l7 14 M").append(x).append(" 432 l7 14 l7 -14\" fill=\"").append(c).append("\" stroke=\"").append(darken(c, .2)).append("\" stroke-width=\"2\"/>");
        }
        s.append("<circle cx=\"370\" cy=\"340\" r=\"62\" fill=\"").append(item.accent()).append("\"/>");
        s.append("<circle cx=\"370\" cy=\"340\" r=\"44\" fill=\"").append(darken(item.accent(), .15)).append("\" opacity=\".5\"/>");
        s.append(brand(260, 220, "#FFFDF7"));
        s.append(label(260, 280, 220, item.label(), item.sub(), "#FFFDF7", 36));
        return s.toString();
    }

    private static String bread(Item item) {
        String c = item.color();
        StringBuilder s = new StringBuilder();
        s.append("<path d=\"M140 300 Q140 190 300 190 Q460 190 460 300 L455 430 Q300 450 145 430 Z\" fill=\"").append(c).append("\"/>");
        s.append("<path d=\"M140 300 Q140 190 300 190 Q460 190 460 300 L455 430 Q300 450 145 430 Z\" fill=\"url(#sheen)\"/>");
        for (int k = 0; k < 4; k++) {
            double x = 200 + k * 65;
            s.append("<path d=\"M").append(f(x)).append(" 225 q25 25 15 60\" stroke=\"").append(lighten(c, .35)).append("\" stroke-width=\"9\" stroke-linecap=\"round\" fill=\"none\"/>");
        }
        s.append("<rect x=\"200\" y=\"340\" width=\"200\" height=\"60\" rx=\"10\" fill=\"#FFFDF7\" opacity=\".95\"/>");
        s.append(label(300, 378, 170, item.label(), null, "#1A1A2E", 24));
        return s.toString();
    }

    private static String fish(Item item) {
        String c = item.color();
        StringBuilder s = new StringBuilder();
        s.append("<path d=\"M120 300 Q250 170 420 280 L500 220 L480 300 L500 380 L420 320 Q250 430 120 300 Z\" fill=\"").append(c).append("\"/>");
        s.append("<path d=\"M120 300 Q250 170 420 280 L500 220 L480 300 L500 380 L420 320 Q250 430 120 300 Z\" fill=\"url(#sheen)\"/>");
        s.append("<path d=\"M140 300 Q260 330 410 300\" stroke=\"").append(darken(c, .2)).append("\" stroke-width=\"3\" fill=\"none\"/>");
        for (int k = 0; k < 8; k++) {
            double x = 210 + k * 26;
            s.append("<path d=\"M").append(f(x)).append(" 260 q12 14 0 28 M").append(f(x)).append(" 305 q12 14 0 28\" stroke=\"").append(lighten(c, .4)).append("\" stroke-width=\"2.5\" fill=\"none\"/>");
        }
        s.append(circle(170, 285, 13, "#FFFFFF")).append(circle(170, 285, 7, "#1A1A2E"));
        s.append("<path d=\"M190 260 Q200 300 190 340\" stroke=\"").append(darken(c, .25)).append("\" stroke-width=\"4\" fill=\"none\"/>");
        s.append("<path d=\"M290 235 L330 200 L350 248 Z\" fill=\"").append(item.accent()).append("\"/>");
        return s.toString();
    }

    private static String meat(Item item, Random r) {
        StringBuilder s = new StringBuilder();
        s.append("<path d=\"M120 260 L480 260 L460 440 L140 440 Z\" fill=\"#1F2430\"/>");
        s.append("<path d=\"M135 270 L465 270 L450 430 L150 430 Z\" fill=\"#2A3040\"/>");
        double[][] pieces = {{220, 330, 70, 48}, {330, 320, 75, 50}, {270, 390, 80, 40}, {390, 385, 60, 38}};
        for (double[] p : pieces) {
            s.append("<ellipse cx=\"").append(f(p[0])).append("\" cy=\"").append(f(p[1])).append("\" rx=\"").append(f(p[2])).append("\" ry=\"").append(f(p[3]))
                    .append("\" fill=\"").append(item.color()).append("\" transform=\"rotate(").append(f((r.nextDouble() - .5) * 30)).append(" ")
                    .append(f(p[0])).append(" ").append(f(p[1])).append(")\"/>");
            s.append("<ellipse cx=\"").append(f(p[0])).append("\" cy=\"").append(f(p[1])).append("\" rx=\"").append(f(p[2])).append("\" ry=\"").append(f(p[3]))
                    .append("\" fill=\"url(#sheen)\"/>");
            s.append("<path d=\"M").append(f(p[0] - p[2] * .5)).append(" ").append(f(p[1] - 6)).append(" q").append(f(p[2] * .5)).append(" -14 ")
                    .append(f(p[2])).append(" 0\" stroke=\"").append(item.accent()).append("\" stroke-width=\"7\" fill=\"none\" opacity=\".8\"/>");
        }
        s.append("<path d=\"M120 260 L480 260 L460 440 L140 440 Z\" fill=\"#fff\" opacity=\".12\"/>");
        s.append("<rect x=\"330\" y=\"230\" width=\"130\" height=\"60\" rx=\"8\" fill=\"#FFFDF7\"/>");
        s.append(label(395, 268, 110, item.label(), null, "#1A1A2E", 20));
        return s.toString();
    }

    private static String soap(Item item) {
        String c = item.color();
        return "<rect x=\"150\" y=\"230\" width=\"300\" height=\"190\" rx=\"70\" fill=\"" + darken(c, .1) + "\"/>"
                + "<rect x=\"150\" y=\"215\" width=\"300\" height=\"190\" rx=\"70\" fill=\"" + c + "\"/>"
                + "<rect x=\"150\" y=\"215\" width=\"300\" height=\"190\" rx=\"70\" fill=\"url(#sheen)\"/>"
                + "<rect x=\"200\" y=\"262\" width=\"200\" height=\"96\" rx=\"40\" fill=\"none\" stroke=\"" + lighten(c, .4) + "\" stroke-width=\"4\"/>"
                + label(300, 318, 160, item.label(), null, darken(c, .35), 30);
    }

    private static String can(Item item) {
        String c = item.color();
        return "<ellipse cx=\"300\" cy=\"140\" rx=\"85\" ry=\"20\" fill=\"#C8CDD2\"/>"
                + "<rect x=\"215\" y=\"140\" width=\"170\" height=\"330\" fill=\"" + c + "\"/>"
                + "<ellipse cx=\"300\" cy=\"470\" rx=\"85\" ry=\"20\" fill=\"" + darken(c, .25) + "\"/>"
                + "<rect x=\"215\" y=\"140\" width=\"170\" height=\"330\" fill=\"url(#side)\"/>"
                + "<path d=\"M215 300 Q300 260 385 300 L385 360 Q300 320 215 360 Z\" fill=\"" + item.accent() + "\"/>"
                + label(300, 230, 150, item.label(), item.sub(), "#FFFDF7", 30);
    }

    // ------------------------------------------------------------------ helpers

    private static String circle(double x, double y, double r, String fill) {
        return "<circle cx=\"" + f(x) + "\" cy=\"" + f(y) + "\" r=\"" + f(r) + "\" fill=\"" + fill + "\"/>";
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;");
    }

    private static String f(double v) {
        return String.format(Locale.ROOT, "%.1f", v).replace(".0", "");
    }

    static String darken(String hex, double amount) {
        return mix(hex, "#000000", amount);
    }

    static String lighten(String hex, double amount) {
        return mix(hex, "#FFFFFF", amount);
    }

    static String mix(String a, String b, double t) {
        int[] x = rgb(a);
        int[] y = rgb(b);
        return String.format("#%02X%02X%02X", Math.round(x[0] + (y[0] - x[0]) * t),
                Math.round(x[1] + (y[1] - x[1]) * t), Math.round(x[2] + (y[2] - x[2]) * t));
    }

    private static int[] rgb(String hex) {
        String h = hex.substring(1);
        return new int[] {Integer.parseInt(h.substring(0, 2), 16), Integer.parseInt(h.substring(2, 4), 16),
            Integer.parseInt(h.substring(4, 6), 16)};
    }
}
