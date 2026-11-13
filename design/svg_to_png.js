/**
 * Rasterizes the original Diamond Rush SVGs into Minecraft-compatible PNG
 * textures. Run from the project root:
 *
 *   node design/svg_to_png.js
 *
 * Requires the `sharp` dev dependency in tools/img (SVG -> PNG, alpha preserved).
 */
const path = require("path");
const fs = require("fs");
const sharp = require(path.join(__dirname, "..", "tools", "img", "node_modules", "sharp"));

const root = path.join(__dirname, "..");
const svgDir = path.join(__dirname, "svg");
const assets = path.join(root, "src", "main", "resources", "assets", "diamondrush");
const guiDir = path.join(assets, "textures", "gui");

const targets = [
  { name: "icon", out: path.join(assets, "icon.png"), width: 128, height: 128 },
  { name: "panel", out: path.join(guiDir, "panel.png"), width: 380, height: 280 },
  { name: "logo", out: path.join(guiDir, "logo.png"), width: 320, height: 56 },
  { name: "champion", out: path.join(guiDir, "champion.png"), width: 84, height: 84 },
  { name: "diamond", out: path.join(guiDir, "diamond.png"), width: 22, height: 22 },
  { name: "scoreboard", out: path.join(guiDir, "scoreboard.png"), width: 240, height: 40 },
  { name: "victory", out: path.join(guiDir, "victory.png"), width: 300, height: 40 },
  { name: "defeat", out: path.join(guiDir, "defeat.png"), width: 300, height: 40 },
  { name: "button_ready", out: path.join(guiDir, "button_ready.png"), width: 220, height: 28 },
  { name: "button_ready_hover", out: path.join(guiDir, "button_ready_hover.png"), width: 220, height: 28 },
  { name: "button_play_again", out: path.join(guiDir, "button_play_again.png"), width: 220, height: 28 },
  { name: "button_play_again_hover", out: path.join(guiDir, "button_play_again_hover.png"), width: 220, height: 28 },
  { name: "button_new_tournament", out: path.join(guiDir, "button_new_tournament.png"), width: 220, height: 28 },
  { name: "button_new_tournament_hover", out: path.join(guiDir, "button_new_tournament_hover.png"), width: 220, height: 28 },
  { name: "button_leave", out: path.join(guiDir, "button_leave.png"), width: 220, height: 28 },
  { name: "button_leave_hover", out: path.join(guiDir, "button_leave_hover.png"), width: 220, height: 28 }
];

(async () => {
  fs.mkdirSync(guiDir, { recursive: true });
  for (const target of targets) {
    const svg = path.join(svgDir, `${target.name}.svg`);
    if (!fs.existsSync(svg)) {
      throw new Error(`Missing SVG source: ${svg}`);
    }
    await sharp(svg, { density: 384 })
      .resize(target.width, target.height, { fit: "fill", kernel: "nearest" })
      .png({ compressionLevel: 9, palette: false })
      .toFile(target.out);
    const stats = fs.statSync(target.out);
    console.log(`${target.name}.svg -> ${path.relative(root, target.out)} (${target.width}x${target.height}, ${stats.size} bytes)`);
  }
  console.log("All PNG textures generated.");
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
