package com.edubas.backend.util;

/**
 * Avatar Generation Utility
 * Generates deterministic, unique avatar SVGs from usernames and emails
 * Mirroring the frontend implementation for consistency
 */
public class AvatarGenerator {

    /**
     * Creates a seeded RNG so the same input always produces the same avatar.
     */
    private static class SeededRNG {
        private long hash;

        public SeededRNG(String seedStr) {
            this.hash = 0;
            for (int i = 0; i < seedStr.length(); i++) {
                hash = seedStr.charAt(i) + ((hash << 5) - hash);
            }
        }

        public double nextDouble() {
            long t = hash += 0x6D2B79F5L;
            t = imul((int) (t ^ (t >>> 15)), (int) (t | 1));
            t ^= t + imul((int) (t ^ (t >>> 7)), (int) (t | 61));
            return (((int) (t ^ (t >>> 14)) & 0xFFFFFFFFL) >>> 0) / 4294967296.0;
        }

        private int imul(int a, int b) {
            return (int) (((long) a * (long) b) & 0xFFFFFFFFL);
        }
    }

    /**
     * Generates a consistent HSL color from a string.
     */
    private static class HSLColor {
        int h, s, l;

        public HSLColor(String str, int s, int l) {
            long hash = 0;
            for (int i = 0; i < str.length(); i++) {
                hash = str.charAt(i) + ((hash << 5) - hash);
            }
            this.h = (int) (hash % 360);
            this.s = s;
            this.l = l;
        }

        public String toCss() {
            return String.format("hsl(%d, %d%%, %d%%)", h, s, l);
        }
    }

    /**
     * Extract initials from a name
     */
    private static String getInitials(String name) {
        if (name == null || name.isEmpty())
            return "";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    /**
     * Generates a smooth blob path using Quadratic Bezier curves.
     */
    private static String generateBlobPath(SeededRNG rng, double cx, double cy, double radius, int complexity,
            double size) {
        double[][] points = new double[complexity][2];
        double angleStep = (Math.PI * 2) / complexity;

        // Generate random points around a circle
        for (int i = 0; i < complexity; i++) {
            double angle = i * angleStep;
            double dist = radius * (0.6 + rng.nextDouble() * 0.8);
            points[i][0] = cx + Math.cos(angle) * dist;
            points[i][1] = cy + Math.sin(angle) * dist;
        }

        // Start building SVG path
        double[] start = {
                (points[complexity - 1][0] + points[0][0]) / 2,
                (points[complexity - 1][1] + points[0][1]) / 2
        };

        StringBuilder d = new StringBuilder();
        d.append(String.format("M %.1f %.1f", start[0], start[1]));

        for (int i = 0; i < complexity; i++) {
            double[] p1 = points[i];
            double[] p2 = points[(i + 1) % complexity];
            double[] mid = { (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2 };
            d.append(String.format(" Q %.1f %.1f %.1f %.1f", p1[0], p1[1], mid[0], mid[1]));
        }

        return d.toString();
    }

    /**
     * Generates an SVG string for an avatar.
     * 
     * @param username  Username of the user
     * @param email     Email of the user
     * @param size      Size of the avatar (default 64)
     * @param type      Avatar type: 'organic' or 'initials'
     * @param isRounded Whether to round the avatar
     * @return SVG string
     */
    public static String generateAvatarSVG(String username, String email, int size, String type, boolean isRounded) {
        String seedStr = (email.toLowerCase().trim() + username.toLowerCase().trim()).isEmpty()
                ? "default"
                : (email.toLowerCase().trim() + username.toLowerCase().trim());

        HSLColor baseColor = new HSLColor(seedStr, 65, 50);
        String safeId = seedStr.replaceAll("[^a-z0-9]", "");
        String viewBox = String.format("0 0 %d %d", size, size);
        double clipRadius = isRounded ? size / 2.0 : size / 8.0;

        String bgColor = String.format("hsl(%d, %d%%, 95%%)", baseColor.h, (int) (baseColor.s * 0.3));

        String content;
        String defs;

        if ("initials".equals(type)) {
            double fontSize = size * 0.4;
            String initials = getInitials(username);

            content = String.format(
                    "<rect width=\"%d\" height=\"%d\" fill=\"%s\" />" +
                            "<text x=\"50%%\" y=\"50%%\" dy=\".1em\" text-anchor=\"middle\" dominant-baseline=\"middle\" "
                            +
                            "font-size=\"%.1f\" fill=\"white\" font-family=\"Arial, sans-serif\" font-weight=\"bold\">%s</text>",
                    size, size, baseColor.toCss(), fontSize, initials);
            defs = "";
        } else {
            // Organic pattern logic
            SeededRNG rng = new SeededRNG(seedStr);
            int layerCount = 3 + (int) (rng.nextDouble() * 2);
            StringBuilder layersSvg = new StringBuilder();

            for (int i = 0; i < layerCount; i++) {
                int complexity = 4 + (int) (rng.nextDouble() * 4);

                double cx = (50 + (rng.nextDouble() - 0.5) * 40) * (size / 100.0);
                double cy = (50 + (rng.nextDouble() - 0.5) * 40) * (size / 100.0);
                double r = (25 + rng.nextDouble() * 25) * (size / 100.0);

                String pathData = generateBlobPath(rng, cx, cy, r, complexity, size);

                double hueShift = (rng.nextDouble() - 0.5) * 60;
                double lightShift = (rng.nextDouble() - 0.5) * 30;
                int newH = (int) (baseColor.h + hueShift);
                int newL = Math.max(20, Math.min(90, (int) (baseColor.l + lightShift)));
                String fill = String.format("hsl(%d, %d%%, %d%%)", newH, baseColor.s, newL);
                double opacity = 0.4 + rng.nextDouble() * 0.4;

                layersSvg.append(String.format(
                        "<path d=\"%s\" fill=\"%s\" opacity=\"%.2f\" style=\"mix-blend-mode: multiply;\" />",
                        pathData, fill, opacity));
            }

            defs = String.format(
                    "<filter id=\"noise-%s\">" +
                            "<feTurbulence type=\"fractalNoise\" baseFrequency=\"0.8\" numOctaves=\"3\" result=\"noise\" />"
                            +
                            "<feColorMatrix type=\"matrix\" values=\"1 0 0 0 0  0 1 0 0 0  0 0 1 0 0  0 0 0 0.4 0\" in=\"noise\" />"
                            +
                            "</filter>" +
                            "<clipPath id=\"clip-%s\">" +
                            "<rect width=\"%d\" height=\"%d\" rx=\"%.1f\" />" +
                            "</clipPath>",
                    safeId, safeId, size, size, clipRadius);

            content = String.format(
                    "<g clip-path=\"url(#clip-%s)\">" +
                            "<rect width=\"%d\" height=\"%d\" fill=\"%s\" />" +
                            "%s" +
                            "<rect width=\"%d\" height=\"%d\" filter=\"url(#noise-%s)\" opacity=\"0.15\" fill=\"transparent\"/>"
                            +
                            "</g>",
                    safeId, size, size, bgColor, layersSvg.toString(), size, size, safeId);
        }

        // Assemble final SVG
        if ("initials".equals(type)) {
            String mask = String.format(
                    "<mask id=\"mask-%s\"><rect width=\"%d\" height=\"%d\" fill=\"white\" rx=\"%.1f\" /></mask>" +
                            "<g mask=\"url(#mask-%s)\">%s</g>",
                    safeId, size, size, clipRadius, safeId, content);
            return String.format(
                    "<svg width=\"%d\" height=\"%d\" viewBox=\"%s\" xmlns=\"http://www.w3.org/2000/svg\">" +
                            "<defs>%s</defs>" +
                            "%s" +
                            "</svg>",
                    size, size, viewBox, defs, mask);
        } else {
            return String.format(
                    "<svg width=\"%d\" height=\"%d\" viewBox=\"%s\" xmlns=\"http://www.w3.org/2000/svg\">" +
                            "<defs>%s</defs>" +
                            "%s" +
                            "</svg>",
                    size, size, viewBox, defs, content);
        }
    }

    /**
     * Generates an SVG string with default settings (organic type, size 64,
     * rounded)
     */
    public static String generateAvatarSVG(String username, String email) {
        return generateAvatarSVG(username, email, 64, "organic", true);
    }
}
