/* @ds-bundle: {"format":4,"namespace":"ChakraUIDesignSystem_349c6d","components":[{"name":"Button","sourcePath":"components/buttons/Button.jsx"},{"name":"IconButton","sourcePath":"components/buttons/IconButton.jsx"},{"name":"Link","sourcePath":"components/buttons/Link.jsx"},{"name":"Avatar","sourcePath":"components/data-display/Avatar.jsx"},{"name":"AvatarGroup","sourcePath":"components/data-display/Avatar.jsx"},{"name":"Badge","sourcePath":"components/data-display/Badge.jsx"},{"name":"Blockquote","sourcePath":"components/data-display/Blockquote.jsx"},{"name":"Card","sourcePath":"components/data-display/Card.jsx"},{"name":"Code","sourcePath":"components/data-display/Code.jsx"},{"name":"Kbd","sourcePath":"components/data-display/Kbd.jsx"},{"name":"List","sourcePath":"components/data-display/List.jsx"},{"name":"Stat","sourcePath":"components/data-display/Stat.jsx"},{"name":"Status","sourcePath":"components/data-display/Status.jsx"},{"name":"Table","sourcePath":"components/data-display/Table.jsx"},{"name":"Tag","sourcePath":"components/data-display/Tag.jsx"},{"name":"Alert","sourcePath":"components/feedback/Alert.jsx"},{"name":"EmptyState","sourcePath":"components/feedback/EmptyState.jsx"},{"name":"Progress","sourcePath":"components/feedback/Progress.jsx"},{"name":"Skeleton","sourcePath":"components/feedback/Skeleton.jsx"},{"name":"SkeletonText","sourcePath":"components/feedback/Skeleton.jsx"},{"name":"Spinner","sourcePath":"components/feedback/Spinner.jsx"},{"name":"Toast","sourcePath":"components/feedback/Toast.jsx"},{"name":"Checkbox","sourcePath":"components/forms/Checkbox.jsx"},{"name":"Field","sourcePath":"components/forms/Field.jsx"},{"name":"INPUT_SIZES","sourcePath":"components/forms/Input.jsx"},{"name":"Input","sourcePath":"components/forms/Input.jsx"},{"name":"NativeSelect","sourcePath":"components/forms/NativeSelect.jsx"},{"name":"PinInput","sourcePath":"components/forms/PinInput.jsx"},{"name":"Radio","sourcePath":"components/forms/RadioGroup.jsx"},{"name":"RadioGroup","sourcePath":"components/forms/RadioGroup.jsx"},{"name":"SegmentGroup","sourcePath":"components/forms/SegmentGroup.jsx"},{"name":"Slider","sourcePath":"components/forms/Slider.jsx"},{"name":"Switch","sourcePath":"components/forms/Switch.jsx"},{"name":"Textarea","sourcePath":"components/forms/Textarea.jsx"},{"name":"Icon","sourcePath":"components/icons/Icon.jsx"},{"name":"ICON_NAMES","sourcePath":"components/icons/Icon.jsx"},{"name":"Container","sourcePath":"components/layout/Container.jsx"},{"name":"Heading","sourcePath":"components/layout/Heading.jsx"},{"name":"Separator","sourcePath":"components/layout/Separator.jsx"},{"name":"Accordion","sourcePath":"components/navigation/Accordion.jsx"},{"name":"Breadcrumb","sourcePath":"components/navigation/Breadcrumb.jsx"},{"name":"Tabs","sourcePath":"components/navigation/Tabs.jsx"},{"name":"Dialog","sourcePath":"components/overlays/Dialog.jsx"},{"name":"Menu","sourcePath":"components/overlays/Menu.jsx"},{"name":"Popover","sourcePath":"components/overlays/Popover.jsx"},{"name":"Tooltip","sourcePath":"components/overlays/Tooltip.jsx"},{"name":"PALETTES","sourcePath":"components/palette.js"},{"name":"TEXT","sourcePath":"components/palette.js"}],"sourceHashes":{"components/buttons/Button.jsx":"e4092bef4861","components/buttons/IconButton.jsx":"1841e2b91cf9","components/buttons/Link.jsx":"cf0d8f7fe605","components/data-display/Avatar.jsx":"8d00a5cdd83b","components/data-display/Badge.jsx":"c8007988ee9c","components/data-display/Blockquote.jsx":"bcfb124e43fb","components/data-display/Card.jsx":"917cc12b202e","components/data-display/Code.jsx":"813b6ed309c1","components/data-display/Kbd.jsx":"ca4901fa1c6f","components/data-display/List.jsx":"5a29ff5c2181","components/data-display/Stat.jsx":"0785ef23be44","components/data-display/Status.jsx":"c60441f1019b","components/data-display/Table.jsx":"1668e5ca6c22","components/data-display/Tag.jsx":"faea48037a37","components/feedback/Alert.jsx":"555b5f464c07","components/feedback/EmptyState.jsx":"239b01cf134b","components/feedback/Progress.jsx":"c719c3ad82df","components/feedback/Skeleton.jsx":"7d7ca4b860bc","components/feedback/Spinner.jsx":"e6159dc56b57","components/feedback/Toast.jsx":"dda18f4b46d1","components/forms/Checkbox.jsx":"adb54f50dea5","components/forms/Field.jsx":"c2fde9adb5ce","components/forms/Input.jsx":"1412ff2efbea","components/forms/NativeSelect.jsx":"912e46d82c46","components/forms/PinInput.jsx":"02b5a2787df1","components/forms/RadioGroup.jsx":"32e571106122","components/forms/SegmentGroup.jsx":"fc9e450c0f85","components/forms/Slider.jsx":"35060393d7dd","components/forms/Switch.jsx":"6e5165c73571","components/forms/Textarea.jsx":"de47802301d4","components/icons/Icon.jsx":"4a04f468a8a8","components/layout/Container.jsx":"575494eb3254","components/layout/Heading.jsx":"fe61f4758909","components/layout/Separator.jsx":"da48f28367b3","components/navigation/Accordion.jsx":"08e6e2352847","components/navigation/Breadcrumb.jsx":"ae6749236516","components/navigation/Tabs.jsx":"f5ac586761d8","components/overlays/Dialog.jsx":"d6fce5eec5bd","components/overlays/Menu.jsx":"57f9ec531c30","components/overlays/Popover.jsx":"a1c3fe49fa75","components/overlays/Tooltip.jsx":"67b7a50ed83a","components/palette.js":"3db7588247f5","ui_kits/website/DocsChrome.jsx":"f9f9ee3b6fb8","ui_kits/website/DocsPage.jsx":"f6c71ecfe5eb","ui_kits/website/HomePage.jsx":"421a3e2d19d9","ui_kits/website/Shared.jsx":"0aec2615417e"},"inlinedExternals":[],"unexposedExports":[{"name":"badgeVariantStyle","sourcePath":"components/data-display/Badge.jsx"},{"name":"buttonVariantStyle","sourcePath":"components/buttons/Button.jsx"},{"name":"cp","sourcePath":"components/palette.js"},{"name":"inputVariantStyle","sourcePath":"components/forms/Input.jsx"},{"name":"mix","sourcePath":"components/palette.js"},{"name":"useFocus","sourcePath":"components/palette.js"},{"name":"useHover","sourcePath":"components/palette.js"}]} */

(() => {

const __ds_ns = (window.ChakraUIDesignSystem_349c6d = window.ChakraUIDesignSystem_349c6d || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/data-display/List.jsx
try { (() => {
function List({
  items = [],
  variant = "marker",
  ordered,
  gap = 8,
  align = "start",
  style
}) {
  const Tag = ordered ? "ol" : "ul";
  const plain = variant === "plain";
  return /*#__PURE__*/React.createElement(Tag, {
    style: {
      display: "flex",
      flexDirection: "column",
      gap,
      margin: 0,
      paddingInlineStart: plain ? 0 : "1.5em",
      listStyle: plain ? "none" : undefined,
      ...style
    }
  }, items.map((it, i) => {
    const o = typeof it === "object" && it !== null && !React.isValidElement(it) ? it : {
      content: it
    };
    return /*#__PURE__*/React.createElement("li", {
      key: i,
      style: {
        whiteSpace: "normal",
        display: plain ? "inline-flex" : "list-item",
        alignItems: {
          start: "flex-start",
          center: "center",
          end: "flex-end"
        }[align],
        "--marker": "var(--fg-subtle)"
      }
    }, o.indicator && /*#__PURE__*/React.createElement("span", {
      style: {
        marginInlineEnd: 8,
        minHeight: "1lh",
        flexShrink: 0,
        display: "inline-flex",
        alignItems: "center",
        "--icon-size": "1em"
      }
    }, o.indicator), /*#__PURE__*/React.createElement("span", null, o.content));
  }));
}
Object.assign(__ds_scope, { List });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/List.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Skeleton.jsx
try { (() => {
function Skeleton({
  variant = "pulse",
  loading = true,
  width,
  height = 16,
  circle,
  children,
  style
}) {
  if (!loading) return /*#__PURE__*/React.createElement("span", {
    style: {
      animation: "chakra-fade-in 0.1s ease-out",
      display: "contents"
    }
  }, children);
  const v = variant === "shine" ? {
    backgroundImage: "linear-gradient(270deg, var(--bg-muted), var(--bg-emphasized), var(--bg-emphasized), var(--bg-muted))",
    backgroundSize: "400% 100%",
    animation: "chakra-bg-position 5s ease-in-out infinite"
  } : variant === "none" ? {
    background: "var(--bg-emphasized)"
  } : {
    background: "var(--bg-emphasized)",
    animation: "chakra-pulse 1.2s cubic-bezier(0.4, 0, 0.6, 1) infinite"
  };
  return /*#__PURE__*/React.createElement("div", {
    "aria-busy": "true",
    style: {
      width: circle ? height : width || "100%",
      height,
      borderRadius: circle ? "9999px" : "var(--radius-l2)",
      flexShrink: 0,
      color: "transparent",
      pointerEvents: "none",
      userSelect: "none",
      ...v,
      ...style
    }
  }, children && /*#__PURE__*/React.createElement("div", {
    style: {
      visibility: "hidden"
    }
  }, children));
}
function SkeletonText({
  noOfLines = 3,
  gap = 8,
  variant
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap,
      width: "100%"
    }
  }, Array.from({
    length: noOfLines
  }).map((_, i) => /*#__PURE__*/React.createElement(Skeleton, {
    key: i,
    variant: variant,
    height: 16,
    width: i === noOfLines - 1 ? "80%" : "100%"
  })));
}
Object.assign(__ds_scope, { Skeleton, SkeletonText });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Skeleton.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Spinner.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const SIZES = {
  inherit: "1em",
  xs: "0.75rem",
  sm: "1rem",
  md: "1.25rem",
  lg: "2rem",
  xl: "2.5rem"
};
function Spinner({
  size = "md",
  color = "currentColor",
  trackColor = "transparent",
  borderWidth = 2,
  label = "Loading...",
  style,
  ...rest
}) {
  const s = SIZES[size] || SIZES.md;
  return /*#__PURE__*/React.createElement("span", _extends({
    role: "status",
    "aria-label": label,
    style: {
      display: "inline-block",
      width: s,
      height: s,
      borderRadius: "9999px",
      borderStyle: "solid",
      borderWidth,
      borderColor: color,
      borderBottomColor: trackColor,
      borderInlineStartColor: trackColor,
      animation: "chakra-spin var(--duration-slowest) linear infinite",
      flexShrink: 0,
      ...style
    }
  }, rest));
}
Object.assign(__ds_scope, { Spinner });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Spinner.jsx", error: String((e && e.message) || e) }); }

// components/icons/Icon.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// Path data copied from Lucide (lucide.dev, ISC) — the icon set chakra-ui.com uses via react-icons/lu.
const PATHS = {
  check: ["M20 6 9 17l-5-5"],
  minus: ["M5 12h14"],
  x: ["M18 6 6 18", "m6 6 12 12"],
  "chevron-down": ["m6 9 6 6 6-6"],
  "chevron-up": ["m18 15-6-6-6 6"],
  "chevron-right": ["m9 18 6-6-6-6"],
  "chevron-left": ["m15 18-6-6 6-6"],
  "chevrons-up-down": ["m7 15 5 5 5-5", "m7 9 5-5 5 5"],
  "arrow-right": ["M5 12h14", "m12 5 7 7-7 7"],
  "arrow-up-right": ["M7 7h10v10", "M7 17 17 7"],
  search: ["m21 21-4.34-4.34", "M11 3a8 8 0 1 0 0 16 8 8 0 0 0 0-16z"],
  info: ["M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M12 16v-4", "M12 8h.01"],
  "circle-check": ["M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "m9 12 2 2 4-4"],
  "circle-alert": ["M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M12 8v4", "M12 16h.01"],
  "triangle-alert": ["m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3", "M12 9v4", "M12 17h.01"],
  terminal: ["m4 17 6-6-6-6", "M12 19h8"],
  sun: ["M12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", "M12 2v2", "M12 20v2", "m4.93 4.93 1.41 1.41", "m17.66 17.66 1.41 1.41", "M2 12h2", "M20 12h2", "m6.34 17.66-1.41 1.41", "m19.07 4.93-1.41 1.41"],
  moon: ["M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"],
  menu: ["M4 12h16", "M4 6h16", "M4 18h16"],
  copy: ["M8 8h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H10a2 2 0 0 1-2-2V10a2 2 0 0 1 2-2z", "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"],
  plus: ["M5 12h14", "M12 5v14"],
  "party-popper": ["M5.8 11.3 2 22l10.7-3.79", "M4 3h.01", "M22 8h.01", "M15 2h.01", "M22 20h.01", "m22 2-2.24.75a2.9 2.9 0 0 0-1.96 3.12c.1.86-.57 1.63-1.45 1.63h-.38c-.86 0-1.6.6-1.76 1.44L14 10", "m22 13-.82-.33c-.86-.34-1.82.2-1.98 1.11c-.11.7-.72 1.22-1.43 1.22H17", "m11 2 .33.82c.34.86-.2 1.82-1.11 1.98C9.52 4.9 9 5.52 9 6.23V7", "M11 13c1.93 1.93 2.83 4.17 2 5-.83.83-3.07-.07-5-2-1.93-1.93-2.83-4.17-2-5 .83-.83 3.07.07 5 2Z"],
  box: ["M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z", "m3.3 7 8.7 5 8.7-5", "M12 22V12"],
  type: ["M4 7V4h16v3", "M9 20h6", "M12 4v16"],
  "paint-bucket": ["m19 11-8-8-8.6 8.6a2 2 0 0 0 0 2.8l5.2 5.2c.8.8 2 .8 2.8 0L19 11Z", "m5 2 5 5", "M2 13h15", "M22 20a2 2 0 1 1-4 0c0-1.6 1.7-2.4 2-4 .3 1.6 2 2.4 2 4Z"],
  inbox: ["M22 12h-6l-2 3h-4l-2-3H2", "M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z"],
  "trending-up": ["M16 7h6v6", "m22 7-8.5 8.5-5-5L2 17"],
  "trending-down": ["M16 17h6v-6", "m22 17-8.5-8.5-5 5L2 7"],
  quote: ["M16 3a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2 1 1 0 0 1 1 1v1a2 2 0 0 1-2 2 1 1 0 0 0-1 1v2a1 1 0 0 0 1 1 6 6 0 0 0 6-6V5a2 2 0 0 0-2-2z", "M5 3a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2 1 1 0 0 1 1 1v1a2 2 0 0 1-2 2 1 1 0 0 0-1 1v2a1 1 0 0 0 1 1 6 6 0 0 0 6-6V5a2 2 0 0 0-2-2z"],
  github: ["M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4", "M9 18c-4.51 2-5-2-7-2"],
  heart: ["M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"],
  settings: ["M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z", "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z"],
  user: ["M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", "M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z"],
  "log-out": ["m16 17 5-5-5-5", "M21 12H9", "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"],
  pencil: ["M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z", "m15 5 4 4"],
  trash: ["M3 6h18", "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"],
  mail: ["M22 7l-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7", "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z"]
};
const SIZES = {
  xs: 12,
  sm: 16,
  md: 20,
  lg: 24,
  xl: 28,
  "2xl": 32
};
function Icon({
  name,
  size = "inherit",
  strokeWidth = 2,
  children,
  style,
  ...rest
}) {
  const px = SIZES[size];
  const dim = px ? px : "var(--icon-size, 1em)";
  const base = {
    display: "inline-block",
    flexShrink: 0,
    verticalAlign: "middle",
    color: "currentColor",
    width: dim,
    height: dim,
    lineHeight: "1em",
    ...style
  };
  if (children) return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      ...base,
      display: "inline-flex"
    }
  }, rest), children);
  const paths = PATHS[name] || [];
  return /*#__PURE__*/React.createElement("svg", _extends({
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: strokeWidth,
    strokeLinecap: "round",
    strokeLinejoin: "round",
    "aria-hidden": "true",
    style: base
  }, rest), paths.map((d, i) => /*#__PURE__*/React.createElement("path", {
    key: i,
    d: d
  })));
}
const ICON_NAMES = Object.keys(PATHS);
Object.assign(__ds_scope, { Icon, ICON_NAMES });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/icons/Icon.jsx", error: String((e && e.message) || e) }); }

// components/layout/Container.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
// maxWidth 8xl (90rem), responsive horizontal padding 16/24/32px at base/md/lg.
function Container({
  fluid,
  centerContent,
  maxWidth,
  children,
  style,
  ...rest
}) {
  const [w, setW] = React.useState(typeof window !== "undefined" ? window.innerWidth : 1280);
  React.useEffect(() => {
    const f = () => setW(window.innerWidth);
    window.addEventListener("resize", f);
    return () => window.removeEventListener("resize", f);
  }, []);
  const px = w >= 1024 ? 32 : w >= 768 ? 24 : 16;
  return /*#__PURE__*/React.createElement("div", _extends({
    style: {
      position: "relative",
      width: "100%",
      marginInline: "auto",
      maxWidth: fluid ? "100%" : maxWidth || "90rem",
      paddingInline: px,
      ...(centerContent ? {
        display: "flex",
        flexDirection: "column",
        alignItems: "center"
      } : {}),
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { Container });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/layout/Container.jsx", error: String((e && e.message) || e) }); }

// components/layout/Separator.jsx
try { (() => {
const T = {
  xs: "0.5px",
  sm: "1px",
  md: "2px",
  lg: "3px"
};
function Separator({
  orientation = "horizontal",
  variant = "solid",
  size = "sm",
  label,
  style
}) {
  const t = T[size] || T.sm;
  if (label && orientation === "horizontal") {
    const line = {
      flex: 1,
      borderTop: `${t} ${variant} var(--border)`
    };
    return /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 12,
        ...style
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: line
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: "var(--font-size-sm)",
        color: "var(--fg-muted)",
        flexShrink: 0
      }
    }, label), /*#__PURE__*/React.createElement("span", {
      style: line
    }));
  }
  const vert = orientation === "vertical";
  return /*#__PURE__*/React.createElement("div", {
    role: "separator",
    "aria-orientation": orientation,
    style: {
      display: "block",
      borderColor: "var(--border)",
      borderStyle: variant,
      borderWidth: 0,
      ...(vert ? {
        borderInlineStartWidth: t,
        alignSelf: "stretch",
        height: "auto"
      } : {
        borderTopWidth: t,
        width: "100%"
      }),
      ...style
    }
  });
}
Object.assign(__ds_scope, { Separator });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/layout/Separator.jsx", error: String((e && e.message) || e) }); }

// components/overlays/Tooltip.jsx
try { (() => {
function Tooltip({
  content,
  placement = "top",
  showArrow,
  open,
  children,
  style
}) {
  const [hover, setHover] = React.useState(false);
  const show = open !== undefined ? open : hover;
  const pos = {
    top: {
      bottom: "100%",
      left: "50%",
      transform: "translateX(-50%)",
      marginBottom: 8
    },
    bottom: {
      top: "100%",
      left: "50%",
      transform: "translateX(-50%)",
      marginTop: 8
    },
    left: {
      right: "100%",
      top: "50%",
      transform: "translateY(-50%)",
      marginRight: 8
    },
    right: {
      left: "100%",
      top: "50%",
      transform: "translateY(-50%)",
      marginLeft: 8
    }
  }[placement];
  const arrow = {
    top: {
      top: "100%",
      left: "50%",
      marginLeft: -4,
      marginTop: -4
    },
    bottom: {
      bottom: "100%",
      left: "50%",
      marginLeft: -4,
      marginBottom: -4
    },
    left: {
      left: "100%",
      top: "50%",
      marginTop: -4,
      marginLeft: -4
    },
    right: {
      right: "100%",
      top: "50%",
      marginTop: -4,
      marginRight: -4
    }
  }[placement];
  return /*#__PURE__*/React.createElement("span", {
    style: {
      position: "relative",
      display: "inline-flex"
    },
    onMouseEnter: () => setHover(true),
    onMouseLeave: () => setHover(false),
    onFocus: () => setHover(true),
    onBlur: () => setHover(false)
  }, children, show && /*#__PURE__*/React.createElement("span", {
    role: "tooltip",
    style: {
      position: "absolute",
      ...pos,
      zIndex: 1800,
      background: "var(--bg-inverted)",
      color: "var(--fg-inverted)",
      padding: "4px 10px",
      borderRadius: "var(--radius-l2)",
      fontWeight: 500,
      fontSize: "var(--font-size-xs)",
      lineHeight: "var(--line-height-xs)",
      boxShadow: "var(--shadow-md)",
      maxWidth: "20rem",
      whiteSpace: "nowrap",
      animation: "chakra-fade-in var(--duration-fast)",
      ...style
    }
  }, content, showArrow && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      width: 8,
      height: 8,
      background: "var(--bg-inverted)",
      transform: "rotate(45deg)",
      ...arrow
    }
  })));
}
Object.assign(__ds_scope, { Tooltip });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/overlays/Tooltip.jsx", error: String((e && e.message) || e) }); }

// components/palette.js
try { (() => {
// Chakra's `colorPalette` mechanism: every recipe styles against colorPalette.{solid,contrast,fg,subtle,muted,emphasized,focusRing,border}.
// cp("teal") returns CSS vars that alias the --cp-* slots to the chosen palette's semantic tokens.
const PALETTES = ["gray", "red", "orange", "yellow", "green", "teal", "blue", "cyan", "purple", "pink"];
function cp(p = "gray") {
  const k = PALETTES.includes(p) ? p : "gray";
  return {
    "--cp-solid": `var(--${k}-solid)`,
    "--cp-contrast": `var(--${k}-contrast)`,
    "--cp-fg": `var(--${k}-fg)`,
    "--cp-subtle": `var(--${k}-subtle)`,
    "--cp-muted": `var(--${k}-muted)`,
    "--cp-emphasized": `var(--${k}-emphasized)`,
    "--cp-focus-ring": `var(--${k}-focus-ring)`,
    "--cp-border": `var(--${k}-border)`
  };
}
const mix = (c, pct) => `color-mix(in srgb, ${c} ${pct}%, transparent)`;
function useHover() {
  const [h, setH] = React.useState(false);
  return [h, {
    onMouseEnter: () => setH(true),
    onMouseLeave: () => setH(false)
  }];
}
function useFocus() {
  const [f, setF] = React.useState(false);
  return [f, {
    onFocus: () => setF(true),
    onBlur: () => setF(false)
  }];
}
// textStyles (theme/text-styles.ts)
const ts = (k, ls) => ({
  fontSize: `var(--font-size-${k})`,
  lineHeight: `var(--line-height-${k})`,
  ...(ls ? {
    letterSpacing: "-0.025em"
  } : {})
});
const TEXT = {
  "2xs": ts("2xs"),
  xs: ts("xs"),
  sm: ts("sm"),
  md: ts("md"),
  lg: ts("lg"),
  xl: ts("xl"),
  "2xl": ts("2xl"),
  "3xl": ts("3xl"),
  "4xl": ts("4xl", 1),
  "5xl": ts("5xl", 1),
  "6xl": ts("6xl", 1),
  "7xl": ts("7xl", 1)
};
Object.assign(__ds_scope, { PALETTES, cp, mix, useHover, useFocus, TEXT });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/palette.js", error: String((e && e.message) || e) }); }

// components/buttons/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const SIZES = {
  "2xs": {
    h: 24,
    px: 8,
    t: "xs",
    gap: 4,
    icon: 14
  },
  xs: {
    h: 32,
    px: 10,
    t: "xs",
    gap: 4,
    icon: 16
  },
  sm: {
    h: 36,
    px: 14,
    t: "sm",
    gap: 8,
    icon: 16
  },
  md: {
    h: 40,
    px: 16,
    t: "sm",
    gap: 8,
    icon: 20
  },
  lg: {
    h: 44,
    px: 20,
    t: "md",
    gap: 12,
    icon: 20
  },
  xl: {
    h: 48,
    px: 20,
    t: "md",
    gap: 10,
    icon: 20
  },
  "2xl": {
    h: 64,
    px: 28,
    t: "lg",
    gap: 12,
    icon: 24
  }
};
function buttonVariantStyle(variant, hover) {
  switch (variant) {
    case "subtle":
      return {
        background: hover ? "var(--cp-muted)" : "var(--cp-subtle)",
        color: "var(--cp-fg)"
      };
    case "surface":
      return {
        background: hover ? "var(--cp-muted)" : "var(--cp-subtle)",
        color: "var(--cp-fg)",
        boxShadow: "0 0 0px 1px var(--cp-muted)"
      };
    case "outline":
      return {
        background: hover ? "var(--cp-subtle)" : "transparent",
        color: "var(--cp-fg)",
        borderColor: "var(--cp-border)"
      };
    case "ghost":
      return {
        background: hover ? "var(--cp-subtle)" : "transparent",
        color: "var(--cp-fg)"
      };
    case "plain":
      return {
        background: "transparent",
        color: "var(--cp-fg)"
      };
    default:
      return {
        background: hover ? __ds_scope.mix("var(--cp-solid)", 90) : "var(--cp-solid)",
        color: "var(--cp-contrast)"
      };
  }
}
function Button({
  variant = "solid",
  size = "md",
  colorPalette = "gray",
  disabled,
  loading,
  loadingText,
  iconOnly,
  children,
  style,
  ...rest
}) {
  const [h, hp] = __ds_scope.useHover();
  const s = SIZES[size] || SIZES.md;
  const off = disabled || loading;
  return /*#__PURE__*/React.createElement("button", _extends({
    className: "chakra-focus-ring",
    disabled: off,
    "aria-busy": loading || undefined
  }, hp, {
    style: {
      ...__ds_scope.cp(colorPalette),
      "--icon-size": s.icon + "px",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      gap: s.gap,
      position: "relative",
      height: s.h,
      minWidth: s.h,
      padding: iconOnly ? 0 : `0 ${s.px}px`,
      ...__ds_scope.TEXT[s.t],
      fontFamily: "inherit",
      fontWeight: 500,
      borderRadius: "var(--radius-l2)",
      border: "1px solid transparent",
      whiteSpace: "nowrap",
      flexShrink: 0,
      userSelect: "none",
      cursor: off ? "not-allowed" : "pointer",
      opacity: disabled ? 0.5 : 1,
      transition: "background 200ms, box-shadow 200ms, color 200ms",
      ...buttonVariantStyle(variant, h && !off),
      ...style
    }
  }, rest), loading && !loadingText ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      inset: 0,
      display: "flex",
      alignItems: "center",
      justifyContent: "center"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Spinner, {
    size: "inherit"
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      visibility: "hidden",
      display: "inline-flex",
      gap: s.gap,
      alignItems: "center"
    }
  }, children)) : loading ? /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(__ds_scope.Spinner, {
    size: "inherit"
  }), loadingText) : children);
}
Object.assign(__ds_scope, { buttonVariantStyle, Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/buttons/Button.jsx", error: String((e && e.message) || e) }); }

// components/buttons/IconButton.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
function IconButton({
  "aria-label": ariaLabel,
  children,
  ...rest
}) {
  return /*#__PURE__*/React.createElement(__ds_scope.Button, _extends({
    iconOnly: true,
    "aria-label": ariaLabel
  }, rest), children);
}
Object.assign(__ds_scope, { IconButton });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/buttons/IconButton.jsx", error: String((e && e.message) || e) }); }

// components/buttons/Link.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
function Link({
  variant = "plain",
  colorPalette = "gray",
  children,
  style,
  ...rest
}) {
  const [h, hp] = __ds_scope.useHover();
  const underline = variant === "underline" || h;
  return /*#__PURE__*/React.createElement("a", _extends({
    className: "chakra-focus-ring"
  }, hp, {
    style: {
      ...__ds_scope.cp(colorPalette),
      display: "inline-flex",
      alignItems: "center",
      gap: 6,
      cursor: "pointer",
      borderRadius: "var(--radius-l1)",
      color: "var(--cp-fg)",
      textDecoration: underline ? "underline" : "none",
      textUnderlineOffset: 3,
      textDecorationColor: __ds_scope.mix("currentColor", 20),
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { Link });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/buttons/Link.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Avatar.jsx
try { (() => {
const SIZES = {
  "2xs": [24, "2xs"],
  xs: [32, "xs"],
  sm: [36, "sm"],
  md: [40, "md"],
  lg: [44, "md"],
  xl: [48, "lg"],
  "2xl": [64, "xl"]
};
const initials = (n = "") => {
  const p = n.trim().split(/\s+/);
  return ((p[0] || "")[0] || "") + (p.length > 1 ? p[p.length - 1][0] || "" : "");
};
function Avatar({
  name,
  src,
  size = "md",
  variant = "subtle",
  shape = "full",
  colorPalette = "gray",
  ring,
  style
}) {
  const [px, fs] = SIZES[size] || SIZES.md;
  const [err, setErr] = React.useState(false);
  const radius = shape === "full" ? "9999px" : shape === "rounded" ? "var(--radius-l3)" : 0;
  const v = variant === "solid" ? {
    background: "var(--cp-solid)",
    color: "var(--cp-contrast)"
  } : variant === "outline" ? {
    color: "var(--cp-fg)",
    border: "1px solid var(--cp-border)"
  } : {
    background: "var(--cp-muted)",
    color: "var(--cp-fg)"
  };
  return /*#__PURE__*/React.createElement("span", {
    style: {
      ...__ds_scope.cp(colorPalette),
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      position: "relative",
      verticalAlign: "top",
      flexShrink: 0,
      userSelect: "none",
      width: px,
      height: px,
      borderRadius: radius,
      fontWeight: 500,
      fontSize: `var(--font-size-${fs})`,
      ...v,
      ...(ring ? {
        border: "2px solid var(--bg)"
      } : {}),
      ...style
    }
  }, src && !err ? /*#__PURE__*/React.createElement("img", {
    src: src,
    alt: name,
    onError: () => setErr(true),
    style: {
      width: "100%",
      height: "100%",
      objectFit: "cover",
      borderRadius: radius
    }
  }) : /*#__PURE__*/React.createElement("span", {
    style: {
      lineHeight: 1,
      textTransform: "uppercase"
    }
  }, initials(name)));
}
function AvatarGroup({
  children,
  spacing = -12,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      ...style
    }
  }, React.Children.map(children, (c, i) => /*#__PURE__*/React.createElement("span", {
    style: {
      marginInlineStart: i ? spacing : 0,
      display: "inline-flex",
      borderRadius: "9999px",
      boxShadow: "0 0 0 2px var(--bg)"
    }
  }, c)));
}
Object.assign(__ds_scope, { Avatar, AvatarGroup });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Avatar.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Badge.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const SIZES = {
  xs: {
    t: "2xs",
    px: 4,
    minH: 16
  },
  sm: {
    t: "xs",
    px: 6,
    minH: 20
  },
  md: {
    t: "sm",
    px: 8,
    minH: 24
  },
  lg: {
    t: "sm",
    px: 10,
    minH: 28
  }
};
function badgeVariantStyle(variant) {
  switch (variant) {
    case "solid":
      return {
        background: "var(--cp-solid)",
        color: "var(--cp-contrast)"
      };
    case "outline":
      return {
        color: "var(--cp-fg)",
        boxShadow: "inset 0 0 0px 1px var(--cp-border)"
      };
    case "surface":
      return {
        background: "var(--cp-subtle)",
        color: "var(--cp-fg)",
        boxShadow: "inset 0 0 0px 1px var(--cp-muted)"
      };
    case "plain":
      return {
        color: "var(--cp-fg)"
      };
    default:
      return {
        background: "var(--cp-subtle)",
        color: "var(--cp-fg)"
      };
  }
}
function Badge({
  variant = "subtle",
  size = "sm",
  colorPalette = "gray",
  children,
  style,
  ...rest
}) {
  const s = SIZES[size] || SIZES.sm;
  return /*#__PURE__*/React.createElement("span", _extends({
    style: {
      ...__ds_scope.cp(colorPalette),
      display: "inline-flex",
      alignItems: "center",
      gap: 4,
      borderRadius: "var(--radius-l2)",
      fontWeight: 500,
      fontVariantNumeric: "tabular-nums",
      whiteSpace: "nowrap",
      userSelect: "none",
      ...__ds_scope.TEXT[s.t],
      padding: `0 ${s.px}px`,
      minHeight: s.minH,
      ...badgeVariantStyle(variant),
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { badgeVariantStyle, Badge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Badge.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Blockquote.jsx
try { (() => {
function Blockquote({
  variant = "subtle",
  justify = "start",
  colorPalette = "gray",
  cite,
  showIcon,
  children,
  style
}) {
  const align = {
    start: "flex-start",
    center: "center",
    end: "flex-end"
  }[justify];
  const border = variant === "solid" ? "4px solid var(--cp-solid)" : variant === "subtle" ? "4px solid var(--cp-muted)" : undefined;
  return /*#__PURE__*/React.createElement("figure", {
    style: {
      ...__ds_scope.cp(colorPalette),
      margin: 0,
      position: "relative",
      display: "flex",
      flexDirection: "column",
      gap: 8,
      alignItems: align,
      textAlign: justify,
      paddingInline: 20,
      borderInlineStart: border,
      ...style
    }
  }, showIcon && /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "quote",
    style: {
      width: 20,
      height: 20,
      color: variant === "subtle" ? "var(--cp-fg)" : "var(--cp-solid)"
    }
  }), /*#__PURE__*/React.createElement("blockquote", {
    style: {
      margin: 0
    }
  }, children), cite && /*#__PURE__*/React.createElement("figcaption", {
    style: {
      ...__ds_scope.TEXT.sm,
      color: "var(--fg-muted)"
    }
  }, "\u2014 ", cite));
}
Object.assign(__ds_scope, { Blockquote });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Blockquote.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Card.jsx
try { (() => {
const PAD = {
  sm: 16,
  md: 24,
  lg: 28
};
const TITLE = {
  sm: "md",
  md: "lg",
  lg: "xl"
};
function Card({
  variant = "outline",
  size = "md",
  title,
  description,
  footer,
  header,
  children,
  style
}) {
  const p = PAD[size] || PAD.md;
  const v = variant === "elevated" ? {
    background: "var(--bg-panel)",
    boxShadow: "var(--shadow-md)"
  } : variant === "subtle" ? {
    background: "var(--bg-muted)"
  } : {
    background: "var(--bg-panel)",
    border: "1px solid var(--border)"
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      position: "relative",
      minWidth: 0,
      overflowWrap: "break-word",
      borderRadius: "var(--radius-l3)",
      color: "var(--fg)",
      textAlign: "start",
      ...v,
      ...style
    }
  }, (title || description || header) && /*#__PURE__*/React.createElement("div", {
    style: {
      paddingInline: p,
      paddingTop: p,
      display: "flex",
      flexDirection: "column",
      gap: 6
    }
  }, header, title && /*#__PURE__*/React.createElement("div", {
    style: {
      fontWeight: 600,
      ...__ds_scope.TEXT[TITLE[size] || "lg"]
    }
  }, title), description && /*#__PURE__*/React.createElement("div", {
    style: {
      color: "var(--fg-muted)",
      fontSize: "var(--font-size-sm)"
    }
  }, description)), children !== undefined && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: p,
      flex: 1,
      display: "flex",
      flexDirection: "column"
    }
  }, children), footer && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      paddingInline: p,
      paddingBottom: p
    }
  }, footer));
}
Object.assign(__ds_scope, { Card });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Card.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Code.jsx
try { (() => {
const SIZES = {
  xs: {
    t: "2xs",
    px: 4,
    minH: 16
  },
  sm: {
    t: "xs",
    px: 6,
    minH: 20
  },
  md: {
    t: "sm",
    px: 8,
    minH: 24
  },
  lg: {
    t: "sm",
    px: 10,
    minH: 28
  }
};
function Code({
  variant = "subtle",
  size = "sm",
  colorPalette = "gray",
  children,
  style
}) {
  const s = SIZES[size] || SIZES.sm;
  return /*#__PURE__*/React.createElement("code", {
    style: {
      ...__ds_scope.cp(colorPalette),
      fontFamily: "var(--font-mono)",
      display: "inline-flex",
      alignItems: "center",
      borderRadius: "var(--radius-l2)",
      ...__ds_scope.TEXT[s.t],
      padding: `0 ${s.px}px`,
      minHeight: s.minH,
      ...__ds_scope.badgeVariantStyle(variant),
      ...style
    }
  }, children);
}
Object.assign(__ds_scope, { Code });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Code.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Kbd.jsx
try { (() => {
const SIZES = {
  sm: {
    t: "xs",
    h: 18
  },
  md: {
    t: "sm",
    h: 20
  },
  lg: {
    t: "md",
    h: 24
  }
};
function Kbd({
  variant = "raised",
  size = "md",
  colorPalette = "gray",
  children,
  style
}) {
  const s = SIZES[size] || SIZES.md;
  const v = variant === "outline" ? {
    border: "1px solid var(--border)",
    color: "var(--cp-fg)"
  } : variant === "subtle" ? {
    background: "var(--cp-muted)",
    color: "var(--cp-fg)"
  } : variant === "plain" ? {
    color: "var(--cp-fg)"
  } : {
    background: "var(--cp-subtle)",
    color: "var(--cp-fg)",
    border: "1px solid var(--cp-muted)",
    borderBottomWidth: 2
  };
  return /*#__PURE__*/React.createElement("kbd", {
    style: {
      ...__ds_scope.cp(colorPalette),
      display: "inline-flex",
      alignItems: "center",
      fontWeight: 500,
      fontFamily: "var(--font-mono)",
      flexShrink: 0,
      whiteSpace: "nowrap",
      wordSpacing: "-0.5em",
      userSelect: "none",
      padding: "0 4px",
      borderRadius: "var(--radius-l2)",
      ...__ds_scope.TEXT[s.t],
      height: s.h,
      ...v,
      ...style
    }
  }, children);
}
Object.assign(__ds_scope, { Kbd });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Kbd.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Stat.jsx
try { (() => {
function Stat({
  label,
  value,
  unit,
  helpText,
  trend,
  trendValue,
  size = "md",
  style
}) {
  const t = {
    sm: "xl",
    md: "2xl",
    lg: "3xl"
  }[size] || "2xl";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 4,
      position: "relative",
      flex: 1,
      ...style
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      gap: 6,
      alignItems: "center",
      color: "var(--fg-muted)",
      ...__ds_scope.TEXT.sm
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      gap: 4,
      alignItems: "baseline",
      fontWeight: 600,
      ...__ds_scope.TEXT[t],
      letterSpacing: "-0.025em",
      fontVariantNumeric: "proportional-nums"
    }
  }, value, unit && /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)",
      ...__ds_scope.TEXT.xs,
      fontWeight: 400,
      letterSpacing: "initial"
    }
  }, unit)), (helpText || trend) && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 8,
      color: "var(--fg-muted)",
      ...__ds_scope.TEXT.xs
    }
  }, trend && /*#__PURE__*/React.createElement(__ds_scope.Badge, {
    colorPalette: trend === "up" ? "green" : "red",
    variant: "plain",
    style: {
      paddingInline: 0
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: trend === "up" ? "trending-up" : "trending-down",
    style: {
      width: "1em",
      height: "1em"
    }
  }), trendValue), helpText));
}
Object.assign(__ds_scope, { Stat });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Stat.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Status.jsx
try { (() => {
function Status({
  value,
  colorPalette,
  size = "md",
  children,
  style
}) {
  const map = {
    success: "green",
    error: "red",
    warning: "orange",
    info: "blue"
  };
  const p = colorPalette || map[value] || "gray";
  const t = {
    sm: "xs",
    md: "sm",
    lg: "md"
  }[size] || "sm";
  return /*#__PURE__*/React.createElement("span", {
    style: {
      ...__ds_scope.cp(p),
      display: "inline-flex",
      alignItems: "center",
      gap: 8,
      ...__ds_scope.TEXT[t],
      ...style
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: "0.64em",
      height: "0.64em",
      flexShrink: 0,
      borderRadius: "9999px",
      background: "var(--cp-solid)"
    }
  }), children);
}
Object.assign(__ds_scope, { Status });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Status.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Table.jsx
try { (() => {
const SIZES = {
  sm: {
    t: "sm",
    px: 8,
    py: 8
  },
  md: {
    t: "sm",
    px: 12,
    py: 12
  },
  lg: {
    t: "md",
    px: 16,
    py: 12
  }
};
function Table({
  columns = [],
  rows = [],
  variant = "line",
  size = "md",
  striped,
  interactive,
  showColumnBorder,
  colorPalette = "gray",
  style
}) {
  const s = SIZES[size] || SIZES.md;
  const [hover, setHover] = React.useState(-1);
  const outline = variant === "outline";
  const cell = last => ({
    padding: `${s.py}px ${s.px}px`,
    textAlign: "start",
    borderBottom: variant === "line" ? "1px solid var(--border)" : undefined,
    borderInlineEnd: showColumnBorder && !last ? "1px solid var(--border)" : undefined
  });
  return /*#__PURE__*/React.createElement("div", {
    style: {
      ...__ds_scope.cp(colorPalette),
      width: "100%",
      overflowX: "auto",
      borderRadius: outline ? "var(--radius-l2)" : undefined,
      boxShadow: outline ? "0 0 0 1px var(--border)" : undefined,
      ...style
    }
  }, /*#__PURE__*/React.createElement("table", {
    style: {
      width: "100%",
      borderCollapse: "collapse",
      fontVariantNumeric: "lining-nums tabular-nums",
      ...__ds_scope.TEXT[s.t]
    }
  }, /*#__PURE__*/React.createElement("thead", {
    style: {
      background: outline ? "var(--bg-muted)" : undefined
    }
  }, /*#__PURE__*/React.createElement("tr", null, columns.map((c, i) => /*#__PURE__*/React.createElement("th", {
    key: c.key || i,
    style: {
      ...cell(i === columns.length - 1),
      fontWeight: 500,
      color: "var(--fg)",
      borderBottom: "1px solid var(--border)",
      textAlign: c.align || "start"
    }
  }, c.label)))), /*#__PURE__*/React.createElement("tbody", null, rows.map((r, ri) => {
    const bg = interactive && hover === ri ? "var(--cp-subtle)" : striped && ri % 2 === 0 ? "var(--bg-muted)" : variant === "line" ? "var(--bg)" : undefined;
    return /*#__PURE__*/React.createElement("tr", {
      key: ri,
      onMouseEnter: () => setHover(ri),
      onMouseLeave: () => setHover(-1),
      style: {
        background: bg,
        borderBottom: outline && ri < rows.length - 1 ? "1px solid var(--border)" : undefined
      }
    }, columns.map((c, i) => /*#__PURE__*/React.createElement("td", {
      key: c.key || i,
      style: {
        ...cell(i === columns.length - 1),
        textAlign: c.align || "start"
      }
    }, r[c.key])));
  }))));
}
Object.assign(__ds_scope, { Table });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Table.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Tag.jsx
try { (() => {
const SIZES = {
  sm: {
    px: 6,
    minH: 18,
    gap: 4,
    el: 12,
    off: -2,
    t: "xs"
  },
  md: {
    px: 6,
    minH: 20,
    gap: 4,
    el: 14,
    off: -2,
    t: "xs"
  },
  lg: {
    px: 8,
    minH: 24,
    gap: 6,
    el: 16,
    off: -3,
    t: "sm"
  },
  xl: {
    px: 10,
    minH: 32,
    gap: 6,
    el: 18,
    off: -4,
    t: "sm"
  }
};
function Tag({
  variant = "surface",
  size = "md",
  colorPalette = "gray",
  startElement,
  endElement,
  onClose,
  children,
  style
}) {
  const s = SIZES[size] || SIZES.md;
  const el = {
    flexShrink: 0,
    width: s.el,
    height: s.el,
    display: "inline-flex",
    "--icon-size": s.el + "px"
  };
  return /*#__PURE__*/React.createElement("span", {
    style: {
      ...__ds_scope.cp(colorPalette),
      display: "inline-flex",
      alignItems: "center",
      verticalAlign: "top",
      maxWidth: "100%",
      userSelect: "none",
      borderRadius: "var(--radius-l2)",
      padding: `0 ${s.px}px`,
      minHeight: s.minH,
      gap: s.gap,
      fontWeight: 500,
      ...__ds_scope.badgeVariantStyle(variant),
      ...style
    }
  }, startElement && /*#__PURE__*/React.createElement("span", {
    style: {
      ...el,
      marginInlineStart: s.off
    }
  }, startElement), /*#__PURE__*/React.createElement("span", {
    style: {
      ...__ds_scope.TEXT[s.t],
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap"
    }
  }, children), endElement && /*#__PURE__*/React.createElement("span", {
    style: {
      ...el,
      marginInlineEnd: s.off
    }
  }, endElement), onClose && /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Remove tag",
    onClick: onClose,
    className: "chakra-focus-ring-inside",
    style: {
      ...el,
      marginInlineEnd: s.off,
      alignItems: "center",
      justifyContent: "center",
      padding: 0,
      border: 0,
      background: "transparent",
      color: "currentColor",
      borderRadius: "var(--radius-l1)",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "x"
  })));
}
Object.assign(__ds_scope, { Tag });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Tag.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Alert.jsx
try { (() => {
const PAL = {
  info: "blue",
  warning: "orange",
  success: "green",
  error: "red",
  neutral: "gray"
};
const ICON = {
  info: "info",
  warning: "triangle-alert",
  success: "circle-check",
  error: "circle-alert",
  neutral: "info"
};
const SIZES = {
  sm: {
    gap: 8,
    p: 12,
    t: "xs",
    it: "lg"
  },
  md: {
    gap: 12,
    p: 16,
    t: "sm",
    it: "xl"
  },
  lg: {
    gap: 12,
    p: 16,
    t: "md",
    it: "2xl"
  }
};
function Alert({
  status = "info",
  variant = "subtle",
  size = "md",
  title,
  children,
  inline,
  icon,
  action,
  style
}) {
  const s = SIZES[size] || SIZES.md;
  const v = variant === "solid" ? {
    background: "var(--cp-solid)",
    color: "var(--cp-contrast)"
  } : variant === "outline" ? {
    color: "var(--cp-fg)",
    boxShadow: "inset 0 0 0px 1px var(--cp-border)"
  } : variant === "surface" ? {
    background: "var(--cp-subtle)",
    color: "var(--cp-fg)",
    boxShadow: "inset 0 0 0px 1px var(--cp-muted)"
  } : {
    background: "var(--cp-subtle)",
    color: "var(--cp-fg)"
  };
  return /*#__PURE__*/React.createElement("div", {
    role: "alert",
    style: {
      ...__ds_scope.cp(PAL[status] || "blue"),
      width: "100%",
      display: "flex",
      alignItems: "flex-start",
      position: "relative",
      borderRadius: "var(--radius-l3)",
      gap: s.gap,
      padding: s.p,
      ...__ds_scope.TEXT[s.t],
      ...v,
      ...style
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      flexShrink: 0,
      width: "1em",
      height: "1em",
      ...__ds_scope.TEXT[s.it],
      lineHeight: 1,
      marginTop: size === "sm" ? 0 : 0
    }
  }, icon || /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: ICON[status],
    style: {
      width: "100%",
      height: "100%"
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flex: 1,
      gap: 4,
      flexDirection: inline ? "row" : "column",
      alignItems: inline ? "center" : undefined
    }
  }, title && /*#__PURE__*/React.createElement("div", {
    style: {
      fontWeight: 500
    }
  }, title), children && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "inline"
    }
  }, children)), action);
}
Object.assign(__ds_scope, { Alert });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Alert.jsx", error: String((e && e.message) || e) }); }

// components/feedback/EmptyState.jsx
try { (() => {
const SIZES = {
  sm: {
    px: 16,
    py: 24,
    title: "md",
    gap: 16,
    ind: "2xl"
  },
  md: {
    px: 32,
    py: 48,
    title: "lg",
    gap: 24,
    ind: "4xl"
  },
  lg: {
    px: 48,
    py: 64,
    title: "xl",
    gap: 32,
    ind: "6xl"
  }
};
function EmptyState({
  icon,
  title,
  description,
  size = "md",
  children,
  style
}) {
  const s = SIZES[size] || SIZES.md;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: "100%",
      padding: `${s.py}px ${s.px}px`,
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      justifyContent: "center",
      gap: s.gap,
      textAlign: "center"
    }
  }, icon && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      color: "var(--fg-subtle)",
      fontSize: `var(--font-size-${s.ind})`,
      "--icon-size": "1em"
    }
  }, icon), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 8,
      alignItems: "center"
    }
  }, title && /*#__PURE__*/React.createElement("div", {
    style: {
      fontWeight: 600,
      ...__ds_scope.TEXT[s.title]
    }
  }, title), description && /*#__PURE__*/React.createElement("div", {
    style: {
      ...__ds_scope.TEXT.sm,
      color: "var(--fg-muted)",
      maxWidth: 360
    }
  }, description)), children));
}
Object.assign(__ds_scope, { EmptyState });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/EmptyState.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Progress.jsx
try { (() => {
const H = {
  xs: 6,
  sm: 8,
  md: 10,
  lg: 12,
  xl: 16
};
function Progress({
  value = 50,
  max = 100,
  size = "md",
  variant = "outline",
  shape = "rounded",
  colorPalette = "gray",
  striped,
  label,
  showValue,
  style
}) {
  const pct = value == null ? null : Math.min(100, Math.max(0, value / max * 100));
  const radius = shape === "full" ? "9999px" : shape === "rounded" ? "var(--radius-l1)" : 0;
  const rangeBg = variant === "subtle" ? __ds_scope.mix("var(--cp-solid)", 72) : "var(--cp-solid)";
  const stripes = striped ? {
    backgroundImage: "linear-gradient(45deg, rgba(255,255,255,0.3) 25%, transparent 25%, transparent 50%, rgba(255,255,255,0.3) 50%, rgba(255,255,255,0.3) 75%, transparent 75%, transparent)",
    backgroundSize: "1rem 1rem"
  } : {};
  return /*#__PURE__*/React.createElement("div", {
    style: {
      ...__ds_scope.cp(colorPalette),
      position: "relative",
      ...__ds_scope.TEXT.sm,
      width: "100%",
      ...style
    }
  }, (label || showValue) && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      marginBottom: 6
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      fontWeight: 500,
      alignItems: "center",
      gap: 4
    }
  }, label), showValue && pct != null && /*#__PURE__*/React.createElement("span", {
    style: {
      ...__ds_scope.TEXT.xs,
      lineHeight: 1,
      fontWeight: 500
    }
  }, Math.round(pct), "%")), /*#__PURE__*/React.createElement("div", {
    role: "progressbar",
    "aria-valuenow": value,
    "aria-valuemax": max,
    style: {
      overflow: "hidden",
      position: "relative",
      height: H[size] || 10,
      borderRadius: radius,
      background: variant === "subtle" ? "var(--cp-muted)" : "var(--bg-muted)",
      boxShadow: variant === "outline" ? "var(--shadow-inset)" : undefined
    }
  }, pct == null ? /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      height: "100%",
      minWidth: "50%",
      backgroundImage: `linear-gradient(to right, transparent 0%, var(--cp-solid) 50%, transparent 100%)`,
      animation: "chakra-position 1s ease infinite"
    }
  }) : /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      width: pct + "%",
      background: rangeBg,
      transition: "width 300ms",
      ...stripes
    }
  })));
}
Object.assign(__ds_scope, { Progress });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Progress.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Toast.jsx
try { (() => {
const ICON = {
  success: "circle-check",
  error: "circle-alert",
  warning: "triangle-alert",
  info: "info"
};
function Toast({
  type = "info",
  title,
  description,
  action,
  onAction,
  onClose,
  style
}) {
  const colored = type === "success" || type === "error" || type === "warning";
  const pal = {
    success: "green",
    error: "red",
    warning: "orange"
  }[type];
  const surface = colored ? {
    background: "var(--cp-solid)",
    color: "var(--cp-contrast)"
  } : {
    background: "var(--bg-panel)",
    color: "var(--fg)"
  };
  const triggerBorder = colored ? "rgba(255,255,255,0.4)" : "var(--border)";
  return /*#__PURE__*/React.createElement("div", {
    role: "status",
    style: {
      ...(pal ? __ds_scope.cp(pal) : {}),
      width: "100%",
      maxWidth: 400,
      display: "flex",
      alignItems: "flex-start",
      position: "relative",
      gap: 12,
      padding: "16px 24px 16px 16px",
      borderRadius: "var(--radius-l2)",
      boxShadow: "var(--shadow-xl)",
      ...surface,
      ...style
    }
  }, type === "loading" ? /*#__PURE__*/React.createElement(__ds_scope.Spinner, {
    size: "md",
    style: {
      marginTop: 0
    }
  }) : (type !== "info" || colored) && /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: ICON[type],
    style: {
      width: 20,
      height: 20,
      flexShrink: 0
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      flexDirection: "column",
      gap: 2
    }
  }, title && /*#__PURE__*/React.createElement("div", {
    style: {
      fontWeight: 500,
      fontSize: "var(--font-size-sm)",
      lineHeight: "var(--line-height-sm)",
      marginInlineEnd: 8
    }
  }, title), description && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--font-size-sm)",
      lineHeight: "var(--line-height-sm)",
      opacity: 0.8
    }
  }, description)), action && /*#__PURE__*/React.createElement("button", {
    type: "button",
    onClick: onAction,
    style: {
      fontFamily: "inherit",
      fontSize: "var(--font-size-sm)",
      fontWeight: 500,
      height: 32,
      padding: "0 12px",
      borderRadius: "var(--radius-l2)",
      alignSelf: "center",
      border: `1px solid ${triggerBorder}`,
      background: "transparent",
      color: "inherit",
      cursor: "pointer"
    }
  }, action), onClose && /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Close",
    onClick: onClose,
    style: {
      position: "absolute",
      top: 4,
      insetInlineEnd: 4,
      padding: 4,
      display: "inline-flex",
      border: 0,
      background: "transparent",
      color: __ds_scope.mix("currentColor", 60),
      borderRadius: "var(--radius-l2)",
      cursor: "pointer",
      fontSize: "var(--font-size-md)"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "x",
    style: {
      width: "1em",
      height: "1em"
    }
  })));
}
Object.assign(__ds_scope, { Toast });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Toast.jsx", error: String((e && e.message) || e) }); }

// components/forms/Checkbox.jsx
try { (() => {
const SIZES = {
  xs: {
    box: 12,
    p: 0,
    gap: 6,
    t: "xs"
  },
  sm: {
    box: 16,
    p: 0,
    gap: 8,
    t: "sm"
  },
  md: {
    box: 20,
    p: 2,
    gap: 10,
    t: "sm"
  },
  lg: {
    box: 24,
    p: 2,
    gap: 12,
    t: "md"
  }
};
function controlStyle(variant, on, invalid) {
  if (variant === "outline") return {
    borderColor: on ? "var(--cp-solid)" : "var(--border)",
    color: "var(--cp-fg)",
    background: "transparent"
  };
  if (variant === "subtle") return {
    background: "var(--cp-muted)",
    borderColor: "var(--cp-muted)",
    color: "var(--cp-fg)"
  };
  return on ? {
    background: "var(--cp-solid)",
    borderColor: "var(--cp-solid)",
    color: "var(--cp-contrast)"
  } : {
    borderColor: invalid ? "var(--border-error)" : "var(--border-emphasized)",
    background: "transparent"
  };
}
function Checkbox({
  checked,
  defaultChecked,
  indeterminate,
  onCheckedChange,
  variant = "solid",
  size = "md",
  colorPalette = "gray",
  disabled,
  invalid,
  children,
  style
}) {
  const [inner, setInner] = React.useState(!!defaultChecked);
  const on = checked !== undefined ? checked : inner;
  const active = on || indeterminate;
  const s = SIZES[size] || SIZES.md;
  const toggle = () => {
    if (disabled) return;
    const n = !on;
    if (checked === undefined) setInner(n);
    onCheckedChange && onCheckedChange(n);
  };
  return /*#__PURE__*/React.createElement("label", {
    style: {
      ...__ds_scope.cp(invalid ? "red" : colorPalette),
      display: "inline-flex",
      alignItems: "center",
      gap: s.gap,
      position: "relative",
      verticalAlign: "top",
      cursor: disabled ? "not-allowed" : "pointer"
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    role: "checkbox",
    "aria-checked": indeterminate ? "mixed" : on,
    disabled: disabled,
    onClick: toggle,
    className: "chakra-focus-ring",
    style: {
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      flexShrink: 0,
      width: s.box,
      height: s.box,
      padding: s.p,
      border: "1px solid transparent",
      borderRadius: "var(--radius-l1)",
      cursor: "inherit",
      opacity: disabled ? 0.5 : 1,
      ...controlStyle(variant, active, invalid),
      ...style
    }
  }, active && /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: indeterminate ? "minus" : "check",
    strokeWidth: 3,
    style: {
      width: "100%",
      height: "100%"
    }
  })), children && /*#__PURE__*/React.createElement("span", {
    style: {
      ...__ds_scope.TEXT[s.t],
      fontWeight: 500,
      userSelect: "none",
      opacity: disabled ? 0.5 : 1
    }
  }, children));
}
Object.assign(__ds_scope, { Checkbox });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Checkbox.jsx", error: String((e && e.message) || e) }); }

// components/forms/Field.jsx
try { (() => {
function Field({
  label,
  helperText,
  errorText,
  required,
  invalid,
  disabled,
  orientation = "vertical",
  labelWidth = 80,
  children,
  style
}) {
  const horiz = orientation === "horizontal";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: horiz ? "row" : "column",
      alignItems: horiz ? "center" : "flex-start",
      justifyContent: horiz ? "space-between" : undefined,
      gap: 6,
      width: "100%",
      position: "relative",
      ...style
    }
  }, label && /*#__PURE__*/React.createElement("label", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 4,
      ...__ds_scope.TEXT.sm,
      fontWeight: 500,
      userSelect: "none",
      opacity: disabled ? 0.5 : 1,
      flex: horiz ? `0 0 ${labelWidth}px` : undefined
    }
  }, label, required && /*#__PURE__*/React.createElement("span", {
    "aria-hidden": "true",
    style: {
      color: "var(--fg-error)",
      lineHeight: 1
    }
  }, "*")), children, invalid && errorText ? /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 4,
      fontWeight: 500,
      color: "var(--fg-error)",
      ...__ds_scope.TEXT.xs
    }
  }, errorText) : helperText ? /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)",
      ...__ds_scope.TEXT.xs
    }
  }, helperText) : null);
}
Object.assign(__ds_scope, { Field });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Field.jsx", error: String((e && e.message) || e) }); }

// components/forms/Input.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const INPUT_SIZES = {
  "2xs": {
    h: 28,
    px: 8,
    t: "xs"
  },
  xs: {
    h: 32,
    px: 8,
    t: "xs"
  },
  sm: {
    h: 36,
    px: 10,
    t: "sm"
  },
  md: {
    h: 40,
    px: 12,
    t: "sm"
  },
  lg: {
    h: 44,
    px: 16,
    t: "md"
  },
  xl: {
    h: 48,
    px: 18,
    t: "md"
  },
  "2xl": {
    h: 64,
    px: 20,
    t: "lg"
  }
};
function inputVariantStyle(variant, focused, invalid) {
  const ring = invalid ? "var(--border-error)" : "var(--cp-focus-ring)";
  if (variant === "flushed") return {
    background: "transparent",
    border: 0,
    borderBottom: "1px solid",
    borderRadius: 0,
    paddingLeft: 0,
    paddingRight: 0,
    borderBottomColor: focused ? ring : invalid ? "var(--border-error)" : "var(--border)",
    boxShadow: focused ? `0px 1px 0px 0px ${ring}` : "none"
  };
  const base = variant === "subtle" ? {
    background: "var(--bg-muted)",
    border: "1px solid transparent"
  } : {
    background: "transparent",
    border: "1px solid var(--border)"
  };
  if (invalid) base.borderColor = "var(--border-error)";
  if (focused) {
    base.outline = `1px solid ${ring}`;
    base.outlineOffset = 0;
    base.borderColor = ring;
  }
  return base;
}
function Input({
  variant = "outline",
  size = "md",
  colorPalette = "gray",
  invalid,
  disabled,
  style,
  onFocus,
  onBlur,
  ...rest
}) {
  const [f, fp] = __ds_scope.useFocus();
  const s = INPUT_SIZES[size] || INPUT_SIZES.md;
  return /*#__PURE__*/React.createElement("input", _extends({
    disabled: disabled,
    "aria-invalid": invalid || undefined,
    onFocus: e => {
      fp.onFocus();
      onFocus && onFocus(e);
    },
    onBlur: e => {
      fp.onBlur();
      onBlur && onBlur(e);
    },
    style: {
      ...__ds_scope.cp(colorPalette),
      width: "100%",
      minWidth: s.h,
      height: s.h,
      padding: `0 ${s.px}px`,
      ...__ds_scope.TEXT[s.t],
      fontFamily: "inherit",
      color: "var(--fg)",
      borderRadius: "var(--radius-l2)",
      outline: 0,
      appearance: "none",
      textAlign: "start",
      opacity: disabled ? 0.5 : 1,
      cursor: disabled ? "not-allowed" : undefined,
      ...inputVariantStyle(variant, f, invalid),
      ...style
    }
  }, rest));
}
Object.assign(__ds_scope, { INPUT_SIZES, inputVariantStyle, Input });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Input.jsx", error: String((e && e.message) || e) }); }

// components/forms/NativeSelect.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const SIZES = {
  xs: {
    h: 32,
    ps: 8,
    pe: 24,
    t: "xs",
    it: "sm",
    ie: 6
  },
  sm: {
    h: 36,
    ps: 10,
    pe: 32,
    t: "sm",
    it: "md",
    ie: 8
  },
  md: {
    h: 40,
    ps: 12,
    pe: 32,
    t: "sm",
    it: "lg",
    ie: 8
  },
  lg: {
    h: 44,
    ps: 16,
    pe: 32,
    t: "md",
    it: "xl",
    ie: 12
  },
  xl: {
    h: 48,
    ps: 18,
    pe: 40,
    t: "md",
    it: "xl",
    ie: 12
  }
};
function NativeSelect({
  variant = "outline",
  size = "md",
  colorPalette = "gray",
  invalid,
  disabled,
  options = [],
  placeholder,
  children,
  style,
  ...rest
}) {
  const [f, fp] = __ds_scope.useFocus();
  const s = SIZES[size] || SIZES.md;
  const v = variant === "ghost" || variant === "plain" ? {
    background: "transparent",
    border: "1px solid transparent",
    ...(f ? {
      outline: "2px solid var(--cp-focus-ring)"
    } : {})
  } : __ds_scope.inputVariantStyle(variant, f, invalid);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      ...__ds_scope.cp(colorPalette),
      position: "relative",
      display: "flex",
      width: "100%",
      height: "fit-content",
      ...style
    }
  }, /*#__PURE__*/React.createElement("select", _extends({
    disabled: disabled,
    "aria-invalid": invalid || undefined
  }, fp, {
    style: {
      width: "100%",
      minWidth: 0,
      height: s.h,
      paddingInlineStart: s.ps,
      paddingInlineEnd: s.pe,
      ...__ds_scope.TEXT[s.t],
      lineHeight: "normal",
      fontFamily: "inherit",
      color: "var(--fg)",
      borderRadius: "var(--radius-l2)",
      outline: 0,
      appearance: "none",
      cursor: "pointer",
      opacity: disabled ? 0.5 : 1,
      ...v
    }
  }, rest), placeholder && /*#__PURE__*/React.createElement("option", {
    value: ""
  }, placeholder), options.map(o => typeof o === "string" ? /*#__PURE__*/React.createElement("option", {
    key: o,
    value: o
  }, o) : /*#__PURE__*/React.createElement("option", {
    key: o.value,
    value: o.value
  }, o.label)), children), /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      insetInlineEnd: s.ie,
      top: "50%",
      transform: "translateY(-50%)",
      display: "inline-flex",
      pointerEvents: "none",
      color: invalid ? "var(--fg-error)" : "var(--fg-muted)",
      fontSize: `var(--font-size-${s.it})`,
      opacity: disabled ? 0.5 : 1
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "chevron-down"
  })));
}
Object.assign(__ds_scope, { NativeSelect });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/NativeSelect.jsx", error: String((e && e.message) || e) }); }

// components/forms/PinInput.jsx
try { (() => {
function PinInput({
  count = 4,
  value,
  defaultValue = "",
  onValueChange,
  size = "md",
  variant = "outline",
  colorPalette = "gray",
  attached,
  mask,
  placeholder = "○",
  style
}) {
  const [inner, setInner] = React.useState(defaultValue);
  const v = (value !== undefined ? value : inner).padEnd(count, " ").slice(0, count);
  const [focus, setFocus] = React.useState(-1);
  const refs = React.useRef([]);
  const s = __ds_scope.INPUT_SIZES[size] || __ds_scope.INPUT_SIZES.md;
  const setAt = (i, ch) => {
    const arr = v.split("");
    arr[i] = ch || " ";
    const n = arr.join("").trimEnd();
    if (value === undefined) setInner(n);
    onValueChange && onValueChange(n);
    if (ch && i < count - 1) refs.current[i + 1] && refs.current[i + 1].focus();
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      ...__ds_scope.cp(colorPalette),
      display: "inline-flex",
      gap: attached ? 0 : 8,
      isolation: "isolate",
      ...style
    }
  }, Array.from({
    length: count
  }).map((_, i) => {
    const c = v[i] === " " ? "" : v[i];
    const r = attached ? {
      borderRadius: i === 0 ? "var(--radius-l2) 0 0 var(--radius-l2)" : i === count - 1 ? "0 var(--radius-l2) var(--radius-l2) 0" : 0,
      marginInlineStart: i ? -1 : 0
    } : {};
    return /*#__PURE__*/React.createElement("input", {
      key: i,
      ref: el => refs.current[i] = el,
      value: c,
      placeholder: focus === i ? "" : placeholder,
      maxLength: 1,
      inputMode: "numeric",
      type: mask ? "password" : "text",
      onFocus: () => setFocus(i),
      onBlur: () => setFocus(-1),
      onChange: e => setAt(i, e.target.value.slice(-1)),
      onKeyDown: e => {
        if (e.key === "Backspace" && !c && i > 0) refs.current[i - 1].focus();
      },
      style: {
        width: s.h,
        height: s.h,
        padding: "0 4px",
        textAlign: "center",
        ...__ds_scope.TEXT[s.t],
        fontFamily: "inherit",
        color: "var(--fg)",
        borderRadius: "var(--radius-l2)",
        outline: 0,
        appearance: "none",
        position: "relative",
        zIndex: focus === i ? 1 : 0,
        ...__ds_scope.inputVariantStyle(variant, focus === i, false),
        ...r
      }
    });
  }));
}
Object.assign(__ds_scope, { PinInput });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/PinInput.jsx", error: String((e && e.message) || e) }); }

// components/forms/RadioGroup.jsx
try { (() => {
const SIZES = {
  xs: {
    box: 12,
    gap: 6,
    t: "xs"
  },
  sm: {
    box: 16,
    gap: 8,
    t: "sm"
  },
  md: {
    box: 20,
    gap: 10,
    t: "sm"
  },
  lg: {
    box: 24,
    gap: 12,
    t: "md"
  }
};
function markStyle(variant, on) {
  if (variant === "outline") return {
    border: "1px solid",
    borderColor: on ? "var(--cp-solid)" : "var(--border)",
    color: on ? "var(--cp-fg)" : "transparent",
    background: "transparent"
  };
  if (variant === "subtle") return {
    border: "1px solid var(--cp-muted)",
    background: "var(--cp-muted)",
    color: on ? "var(--cp-fg)" : "transparent"
  };
  return on ? {
    border: "1px solid var(--cp-solid)",
    background: "var(--cp-solid)",
    color: "var(--cp-contrast)"
  } : {
    border: "1px solid var(--border-emphasized)",
    background: "transparent",
    color: "transparent"
  };
}
function Radio({
  checked,
  onSelect,
  variant = "solid",
  size = "md",
  disabled,
  children,
  value
}) {
  const s = SIZES[size] || SIZES.md;
  return /*#__PURE__*/React.createElement("label", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: s.gap,
      ...__ds_scope.TEXT[s.t],
      fontWeight: 500,
      cursor: disabled ? "not-allowed" : "pointer"
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    role: "radio",
    "aria-checked": !!checked,
    disabled: disabled,
    onClick: () => !disabled && onSelect && onSelect(value),
    className: "chakra-focus-ring",
    style: {
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      flexShrink: 0,
      width: s.box,
      height: s.box,
      padding: 0,
      borderRadius: "9999px",
      cursor: "inherit",
      opacity: disabled ? 0.5 : 1,
      ...markStyle(variant, checked)
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: "100%",
      height: "100%",
      borderRadius: "9999px",
      background: "currentColor",
      transform: `scale(${variant === "outline" ? 0.6 : 0.4})`
    }
  })), children && /*#__PURE__*/React.createElement("span", {
    style: {
      userSelect: "none",
      opacity: disabled ? 0.5 : 1
    }
  }, children));
}
function RadioGroup({
  value,
  defaultValue,
  onValueChange,
  options = [],
  variant = "solid",
  size = "md",
  colorPalette = "gray",
  orientation = "horizontal",
  gap = 24,
  disabled,
  style
}) {
  const [inner, setInner] = React.useState(defaultValue);
  const v = value !== undefined ? value : inner;
  const pick = n => {
    if (value === undefined) setInner(n);
    onValueChange && onValueChange(n);
  };
  return /*#__PURE__*/React.createElement("div", {
    role: "radiogroup",
    style: {
      ...__ds_scope.cp(colorPalette),
      display: "flex",
      flexDirection: orientation === "vertical" ? "column" : "row",
      gap: orientation === "vertical" ? 12 : gap,
      ...style
    }
  }, options.map(o => {
    const val = typeof o === "string" ? o : o.value;
    const label = typeof o === "string" ? o : o.label;
    return /*#__PURE__*/React.createElement(Radio, {
      key: val,
      value: val,
      checked: v === val,
      onSelect: pick,
      variant: variant,
      size: size,
      disabled: disabled || o.disabled
    }, label);
  }));
}
Object.assign(__ds_scope, { Radio, RadioGroup });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/RadioGroup.jsx", error: String((e && e.message) || e) }); }

// components/forms/SegmentGroup.jsx
try { (() => {
const SIZES = {
  xs: {
    h: 24,
    px: 12,
    t: "xs",
    gap: 4
  },
  sm: {
    h: 32,
    px: 16,
    t: "sm",
    gap: 8
  },
  md: {
    h: 40,
    px: 16,
    t: "sm",
    gap: 8
  },
  lg: {
    h: 44,
    px: 18,
    t: "md",
    gap: 12
  }
};
function SegmentGroup({
  items = [],
  value,
  defaultValue,
  onValueChange,
  size = "md",
  disabled,
  style
}) {
  const [inner, setInner] = React.useState(defaultValue !== undefined ? defaultValue : items[0] && (items[0].value || items[0]));
  const v = value !== undefined ? value : inner;
  const s = SIZES[size] || SIZES.md;
  const vals = items.map(it => typeof it === "string" ? it : it.value);
  const sel = vals.indexOf(v);
  return /*#__PURE__*/React.createElement("div", {
    role: "radiogroup",
    style: {
      display: "inline-flex",
      position: "relative",
      isolation: "isolate",
      borderRadius: "var(--radius-l2)",
      boxShadow: "var(--shadow-inset)",
      background: "var(--bg-muted)",
      minWidth: "max-content",
      textAlign: "center",
      ...style
    }
  }, items.map((it, i) => {
    const val = vals[i];
    const label = typeof it === "string" ? it : it.label;
    const on = i === sel;
    const hideSep = i === 0 || on || i - 1 === sel;
    return /*#__PURE__*/React.createElement("button", {
      key: val,
      type: "button",
      role: "radio",
      "aria-checked": on,
      disabled: disabled,
      onClick: () => {
        if (value === undefined) setInner(val);
        onValueChange && onValueChange(val);
      },
      className: "chakra-focus-ring",
      style: {
        position: "relative",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        gap: s.gap,
        height: s.h,
        padding: `0 ${s.px}px`,
        ...__ds_scope.TEXT[s.t],
        fontFamily: "inherit",
        color: "var(--fg)",
        border: 0,
        cursor: disabled ? "not-allowed" : "pointer",
        userSelect: "none",
        opacity: disabled ? 0.5 : 1,
        borderRadius: "var(--radius-l2)",
        background: on ? "var(--bg)" : "transparent",
        boxShadow: on ? "var(--shadow-sm)" : "none",
        transition: "background 200ms, box-shadow 200ms"
      }
    }, /*#__PURE__*/React.createElement("span", {
      "aria-hidden": "true",
      style: {
        position: "absolute",
        insetInlineStart: 0,
        top: 6,
        bottom: 6,
        width: 1,
        background: "var(--border)",
        opacity: hideSep ? 0 : 1,
        transition: "opacity 0.2s"
      }
    }), label);
  }));
}
Object.assign(__ds_scope, { SegmentGroup });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/SegmentGroup.jsx", error: String((e && e.message) || e) }); }

// components/forms/Slider.jsx
try { (() => {
const SIZES = {
  sm: {
    thumb: 16,
    track: 6
  },
  md: {
    thumb: 20,
    track: 8
  },
  lg: {
    thumb: 24,
    track: 10
  }
};
function Slider({
  value,
  defaultValue = 40,
  min = 0,
  max = 100,
  step = 1,
  onValueChange,
  size = "md",
  variant = "outline",
  colorPalette = "gray",
  label,
  showValue,
  disabled,
  style
}) {
  const [inner, setInner] = React.useState(defaultValue);
  const v = value !== undefined ? value : inner;
  const [focus, setFocus] = React.useState(false);
  const s = SIZES[size] || SIZES.md;
  const pct = (v - min) / (max - min) * 100;
  const ref = React.useRef(null);
  const set = n => {
    const c = Math.min(max, Math.max(min, Math.round(n / step) * step));
    if (value === undefined) setInner(c);
    onValueChange && onValueChange(c);
  };
  const fromEvent = e => {
    const r = ref.current.getBoundingClientRect();
    set(min + (e.clientX - r.left) / r.width * (max - min));
  };
  const down = e => {
    if (disabled) return;
    fromEvent(e);
    const mv = ev => fromEvent(ev);
    const up = () => {
      window.removeEventListener("pointermove", mv);
      window.removeEventListener("pointerup", up);
    };
    window.addEventListener("pointermove", mv);
    window.addEventListener("pointerup", up);
  };
  const outline = variant === "outline";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      ...__ds_scope.cp(colorPalette),
      display: "flex",
      flexDirection: "column",
      gap: 4,
      ...__ds_scope.TEXT.sm,
      position: "relative",
      isolation: "isolate",
      touchAction: "none",
      opacity: disabled ? 0.5 : 1,
      width: "100%",
      ...style
    }
  }, (label || showValue) && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontWeight: 500
    }
  }, label), showValue && /*#__PURE__*/React.createElement("span", null, v)), /*#__PURE__*/React.createElement("div", {
    ref: ref,
    onPointerDown: down,
    style: {
      position: "relative",
      display: "flex",
      alignItems: "center",
      minHeight: s.thumb,
      cursor: disabled ? "not-allowed" : "pointer"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      height: s.track,
      borderRadius: "9999px",
      overflow: "hidden",
      background: outline ? __ds_scope.mix("var(--bg-emphasized)", 72) : "var(--cp-subtle)",
      boxShadow: outline ? "var(--shadow-inset)" : undefined
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: pct + "%",
      height: "100%",
      background: disabled ? "var(--border-emphasized)" : "var(--cp-solid)"
    }
  })), /*#__PURE__*/React.createElement("div", {
    role: "slider",
    tabIndex: disabled ? -1 : 0,
    "aria-valuenow": v,
    "aria-valuemin": min,
    "aria-valuemax": max,
    onFocus: () => setFocus(true),
    onBlur: () => setFocus(false),
    onKeyDown: e => {
      if (e.key === "ArrowRight" || e.key === "ArrowUp") set(v + step);
      if (e.key === "ArrowLeft" || e.key === "ArrowDown") set(v - step);
    },
    style: {
      position: "absolute",
      left: `calc(${pct}% - ${pct / 100 * s.thumb}px)`,
      top: "50%",
      translate: "0 -50%",
      width: s.thumb,
      height: s.thumb,
      borderRadius: "9999px",
      zIndex: 2,
      outline: 0,
      background: outline ? "var(--bg)" : "var(--cp-solid)",
      border: outline ? "2px solid var(--cp-solid)" : 0,
      boxShadow: focus ? `0 0 0 3px ${__ds_scope.mix("var(--cp-focus-ring)", 50)}` : undefined,
      transition: "box-shadow 150ms"
    }
  })));
}
Object.assign(__ds_scope, { Slider });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Slider.jsx", error: String((e && e.message) || e) }); }

// components/forms/Switch.jsx
try { (() => {
const SIZES = {
  xs: {
    w: 24,
    h: 12
  },
  sm: {
    w: 32,
    h: 16
  },
  md: {
    w: 40,
    h: 20
  },
  lg: {
    w: 48,
    h: 24
  }
};
function Switch({
  checked,
  defaultChecked,
  onCheckedChange,
  variant = "solid",
  size = "md",
  colorPalette = "gray",
  disabled,
  invalid,
  children
}) {
  const [inner, setInner] = React.useState(!!defaultChecked);
  const on = checked !== undefined ? checked : inner;
  const s = SIZES[size] || SIZES.md;
  const toggle = () => {
    if (disabled) return;
    const n = !on;
    if (checked === undefined) setInner(n);
    onCheckedChange && onCheckedChange(n);
  };
  const diff = s.w - s.h;
  const raised = variant === "raised";
  return /*#__PURE__*/React.createElement("label", {
    style: {
      ...__ds_scope.cp(colorPalette),
      display: "inline-flex",
      alignItems: "center",
      gap: 10,
      verticalAlign: "middle",
      cursor: disabled ? "not-allowed" : "pointer"
    }
  }, /*#__PURE__*/React.createElement("button", {
    type: "button",
    role: "switch",
    "aria-checked": on,
    disabled: disabled,
    onClick: toggle,
    className: "chakra-focus-ring",
    style: {
      position: "relative",
      display: "inline-flex",
      alignItems: raised ? "center" : "flex-start",
      flexShrink: 0,
      padding: 0,
      border: 0,
      cursor: "inherit",
      width: s.w,
      height: raised ? s.h / 2 : s.h,
      borderRadius: "9999px",
      opacity: disabled ? 0.5 : 1,
      transition: "background 150ms",
      background: raised ? on ? __ds_scope.mix("var(--cp-solid)", 60) : "var(--bg-muted)" : on ? "var(--cp-solid)" : "var(--bg-emphasized)",
      boxShadow: raised ? "var(--shadow-inset)" : undefined,
      outline: invalid ? "2px solid var(--border-error)" : undefined,
      outlineOffset: invalid ? 2 : undefined
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: s.h,
      height: s.h,
      borderRadius: "9999px",
      flexShrink: 0,
      transition: "translate 150ms",
      translate: on ? `${diff}px 0` : "0 0",
      scale: raised ? "1" : "0.8",
      background: raised ? on ? "var(--cp-solid)" : "white" : on ? "var(--cp-contrast)" : "white",
      boxShadow: raised ? "var(--shadow-xs)" : "var(--shadow-sm)"
    }
  })), children && /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--font-size-sm)",
      fontWeight: 500,
      lineHeight: 1,
      userSelect: "none",
      opacity: disabled ? 0.5 : 1
    }
  }, children));
}
Object.assign(__ds_scope, { Switch });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Switch.jsx", error: String((e && e.message) || e) }); }

// components/forms/Textarea.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const SIZES = {
  xs: {
    px: 8,
    py: 6,
    t: "xs"
  },
  sm: {
    px: 10,
    py: 8,
    t: "sm"
  },
  md: {
    px: 12,
    py: 8,
    t: "sm"
  },
  lg: {
    px: 16,
    py: 12,
    t: "md"
  },
  xl: {
    px: 18,
    py: 14,
    t: "md"
  }
};
function Textarea({
  variant = "outline",
  size = "md",
  colorPalette = "gray",
  invalid,
  disabled,
  rows = 3,
  style,
  ...rest
}) {
  const [f, fp] = __ds_scope.useFocus();
  const s = SIZES[size] || SIZES.md;
  return /*#__PURE__*/React.createElement("textarea", _extends({
    rows: rows,
    disabled: disabled,
    "aria-invalid": invalid || undefined
  }, fp, {
    style: {
      ...__ds_scope.cp(colorPalette),
      width: "100%",
      minWidth: 0,
      padding: `${s.py}px ${s.px}px`,
      ...__ds_scope.TEXT[s.t],
      fontFamily: "inherit",
      color: "var(--fg)",
      borderRadius: "var(--radius-l2)",
      outline: 0,
      appearance: "none",
      resize: "vertical",
      opacity: disabled ? 0.5 : 1,
      ...__ds_scope.inputVariantStyle(variant, f, invalid),
      ...(variant === "flushed" ? {
        paddingLeft: 0,
        paddingRight: 0
      } : {}),
      ...style
    }
  }, rest));
}
Object.assign(__ds_scope, { Textarea });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Textarea.jsx", error: String((e && e.message) || e) }); }

// components/layout/Heading.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
function Heading({
  as = "h2",
  size = "xl",
  children,
  style,
  ...rest
}) {
  const Tag = as;
  return /*#__PURE__*/React.createElement(Tag, _extends({
    style: {
      margin: 0,
      fontFamily: "var(--font-heading)",
      fontWeight: 600,
      ...(__ds_scope.TEXT[size] || __ds_scope.TEXT.xl),
      ...style
    }
  }, rest), children);
}
Object.assign(__ds_scope, { Heading });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/layout/Heading.jsx", error: String((e && e.message) || e) }); }

// components/navigation/Accordion.jsx
try { (() => {
const SIZES = {
  sm: {
    px: 12,
    py: 8,
    t: "sm"
  },
  md: {
    px: 16,
    py: 8,
    t: "md"
  },
  lg: {
    px: 18,
    py: 10,
    t: "lg"
  }
};
function Accordion({
  items = [],
  defaultValue = [],
  multiple,
  collapsible = true,
  variant = "outline",
  size = "md",
  colorPalette = "gray",
  style
}) {
  const [open, setOpen] = React.useState(defaultValue);
  const s = SIZES[size] || SIZES.md;
  const toggle = v => setOpen(o => o.includes(v) ? collapsible || multiple ? o.filter(x => x !== v) : o : multiple ? [...o, v] : [v]);
  const padX = variant === "subtle" || variant === "enclosed" ? s.px : 0;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      ...__ds_scope.cp(colorPalette),
      width: "100%",
      ...(variant === "enclosed" ? {
        border: "1px solid var(--border)",
        borderRadius: "var(--radius-l2)",
        overflow: "hidden"
      } : {}),
      ...style
    }
  }, items.map((it, i) => {
    const on = open.includes(it.value);
    const itemStyle = variant === "outline" ? {
      borderBottom: "1px solid var(--border)"
    } : variant === "subtle" ? {
      borderRadius: "var(--radius-l2)",
      background: on ? "var(--cp-subtle)" : undefined
    } : variant === "enclosed" ? {
      borderTop: i ? "1px solid var(--border)" : undefined,
      background: on ? "var(--bg-subtle)" : undefined
    } : {};
    return /*#__PURE__*/React.createElement("div", {
      key: it.value,
      style: itemStyle
    }, /*#__PURE__*/React.createElement("button", {
      type: "button",
      "aria-expanded": on,
      disabled: it.disabled,
      onClick: () => toggle(it.value),
      className: "chakra-focus-ring",
      style: {
        display: "flex",
        alignItems: "center",
        textAlign: "start",
        width: "100%",
        gap: 12,
        fontWeight: 500,
        fontFamily: "inherit",
        color: "var(--fg)",
        padding: `${s.py}px ${padX}px`,
        ...__ds_scope.TEXT[s.t],
        border: 0,
        background: "transparent",
        borderRadius: "var(--radius-l2)",
        cursor: it.disabled ? "not-allowed" : "pointer",
        opacity: it.disabled ? 0.5 : 1
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        flex: 1
      }
    }, it.title), /*#__PURE__*/React.createElement(__ds_scope.Icon, {
      name: "chevron-down",
      style: {
        width: "1.2em",
        height: "1.2em",
        color: "var(--fg-subtle)",
        transition: "rotate 0.2s",
        rotate: on ? "180deg" : "0deg"
      }
    })), on && /*#__PURE__*/React.createElement("div", {
      style: {
        overflow: "hidden",
        padding: `0 ${padX}px`,
        animation: "chakra-fade-in var(--duration-moderate)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        paddingTop: s.py,
        paddingBottom: s.py * 2,
        ...__ds_scope.TEXT.sm,
        color: "var(--fg)"
      }
    }, it.content)));
  }));
}
Object.assign(__ds_scope, { Accordion });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/Accordion.jsx", error: String((e && e.message) || e) }); }

// components/navigation/Breadcrumb.jsx
try { (() => {
function Breadcrumb({
  items = [],
  variant = "plain",
  size = "md",
  colorPalette = "gray",
  separator,
  style
}) {
  const s = {
    sm: {
      gap: 4,
      t: "xs"
    },
    md: {
      gap: 6,
      t: "sm"
    },
    lg: {
      gap: 8,
      t: "md"
    }
  }[size] || {
    gap: 6,
    t: "sm"
  };
  return /*#__PURE__*/React.createElement("nav", {
    "aria-label": "breadcrumb",
    style: {
      ...__ds_scope.cp(colorPalette),
      ...style
    }
  }, /*#__PURE__*/React.createElement("ol", {
    style: {
      display: "flex",
      alignItems: "center",
      flexWrap: "wrap",
      gap: s.gap,
      ...__ds_scope.TEXT[s.t],
      color: "var(--fg-muted)",
      listStyle: "none",
      margin: 0,
      padding: 0
    }
  }, items.map((it, i) => {
    const last = i === items.length - 1;
    const linkColor = variant === "underline" ? "var(--cp-fg)" : last ? "var(--fg)" : "var(--fg-muted)";
    return /*#__PURE__*/React.createElement(React.Fragment, {
      key: i
    }, /*#__PURE__*/React.createElement("li", {
      style: {
        display: "inline-flex",
        alignItems: "center"
      }
    }, last ? /*#__PURE__*/React.createElement("span", {
      "aria-current": "page",
      style: {
        color: variant === "underline" ? "var(--cp-fg)" : "var(--fg)"
      }
    }, it.label) : /*#__PURE__*/React.createElement("a", {
      href: it.href || "#",
      onClick: it.onClick,
      className: "chakra-focus-ring",
      style: {
        display: "inline-flex",
        alignItems: "center",
        gap: 8,
        borderRadius: "var(--radius-l1)",
        color: linkColor,
        textDecoration: variant === "underline" ? "underline" : "none",
        textUnderlineOffset: "0.2em",
        textDecorationColor: "var(--cp-muted)"
      }
    }, it.icon, it.label)), !last && /*#__PURE__*/React.createElement("li", {
      "aria-hidden": "true",
      style: {
        color: "var(--fg-muted)",
        opacity: 0.8,
        display: "inline-flex"
      }
    }, separator || /*#__PURE__*/React.createElement(__ds_scope.Icon, {
      name: "chevron-right",
      style: {
        width: "1em",
        height: "1em"
      }
    })));
  })));
}
Object.assign(__ds_scope, { Breadcrumb });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/Breadcrumb.jsx", error: String((e && e.message) || e) }); }

// components/navigation/Tabs.jsx
try { (() => {
const SIZES = {
  sm: {
    h: 36,
    py: 4,
    px: 12,
    t: "sm",
    pad: 12
  },
  md: {
    h: 40,
    py: 8,
    px: 16,
    t: "sm",
    pad: 16
  },
  lg: {
    h: 44,
    py: 8,
    px: 18,
    t: "md",
    pad: 18
  }
};
function Tabs({
  items = [],
  value,
  defaultValue,
  onValueChange,
  variant = "line",
  size = "md",
  colorPalette = "gray",
  fitted,
  justify = "start",
  children,
  style,
  listStyle
}) {
  const [inner, setInner] = React.useState(defaultValue !== undefined ? defaultValue : items[0] && items[0].value);
  const v = value !== undefined ? value : inner;
  const s = SIZES[size] || SIZES.md;
  const pick = n => {
    if (value === undefined) setInner(n);
    onValueChange && onValueChange(n);
  };
  const list = {
    line: {
      display: "flex",
      borderBottom: "1px solid var(--border)"
    },
    enclosed: {
      background: "var(--bg-muted)",
      padding: 4,
      borderRadius: "var(--radius-l3)",
      minHeight: s.h - 4
    },
    outline: {
      display: "flex",
      borderBottom: "1px solid var(--border)"
    }
  }[variant] || {};
  const trig = on => {
    const base = {
      color: on ? "var(--fg)" : "var(--fg-muted)"
    };
    if (variant === "line") return {
      ...base,
      boxShadow: on ? "inset 0 -2px 0 var(--cp-solid)" : undefined,
      marginBottom: -1
    };
    if (variant === "subtle") return {
      ...base,
      borderRadius: "var(--radius-l2)",
      background: on ? "var(--cp-subtle)" : "transparent",
      color: on ? "var(--cp-fg)" : "var(--fg-muted)"
    };
    if (variant === "enclosed") return {
      ...base,
      justifyContent: "center",
      borderRadius: "var(--radius-l2)",
      background: on ? "var(--bg)" : "transparent",
      color: on ? "var(--cp-fg)" : "var(--fg-muted)",
      boxShadow: on ? "var(--shadow-xs)" : undefined
    };
    if (variant === "outline") return {
      ...base,
      border: "1px solid",
      borderColor: on ? "var(--border)" : "transparent",
      borderBottomColor: on ? "var(--bg)" : "transparent",
      borderRadius: "var(--radius-l2) var(--radius-l2) 0 0",
      marginBottom: -1,
      background: on ? "var(--bg)" : "transparent",
      color: on ? "var(--cp-fg)" : "var(--fg-muted)"
    };
    return {
      ...base,
      color: on ? "var(--cp-fg)" : "var(--fg-muted)",
      borderRadius: "var(--radius-l2)"
    };
  };
  const active = items.find(it => it.value === v);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      ...__ds_scope.cp(colorPalette),
      position: "relative",
      ...style
    }
  }, /*#__PURE__*/React.createElement("div", {
    role: "tablist",
    style: {
      display: fitted ? "flex" : "inline-flex",
      position: "relative",
      isolation: "isolate",
      minHeight: s.h,
      justifyContent: {
        start: "flex-start",
        center: "center",
        end: "flex-end"
      }[justify],
      ...list,
      ...(variant === "line" || variant === "outline" || fitted ? {
        display: "flex"
      } : {}),
      ...listStyle
    }
  }, items.map(it => {
    const on = it.value === v;
    return /*#__PURE__*/React.createElement("button", {
      key: it.value,
      role: "tab",
      "aria-selected": on,
      disabled: it.disabled,
      onClick: () => pick(it.value),
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8,
        minWidth: s.h,
        height: s.h,
        padding: `${s.py}px ${s.px}px`,
        ...__ds_scope.TEXT[s.t],
        fontFamily: "inherit",
        fontWeight: 500,
        position: "relative",
        cursor: it.disabled ? "not-allowed" : "pointer",
        opacity: it.disabled ? 0.5 : 1,
        border: 0,
        background: "transparent",
        outline: 0,
        flex: fitted ? 1 : undefined,
        justifyContent: fitted ? "center" : undefined,
        transition: "color 200ms, background 200ms",
        "--icon-size": "16px",
        ...trig(on)
      }
    }, it.icon, it.label);
  })), active && active.content !== undefined && /*#__PURE__*/React.createElement("div", {
    role: "tabpanel",
    style: {
      paddingTop: s.pad,
      width: "100%"
    }
  }, active.content), children);
}
Object.assign(__ds_scope, { Tabs });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/Tabs.jsx", error: String((e && e.message) || e) }); }

// components/overlays/Dialog.jsx
try { (() => {
const MAXW = {
  xs: "24rem",
  sm: "28rem",
  md: "32rem",
  lg: "42rem",
  xl: "56rem"
};

// Renders inline (no portal) when `contained` is set — handy for cards & static mocks.
function Dialog({
  open = true,
  onOpenChange,
  title,
  description,
  children,
  footer,
  size = "md",
  placement = "top",
  contained,
  showClose = true,
  style
}) {
  if (!open) return null;
  const close = () => onOpenChange && onOpenChange(false);
  const pos = contained ? "absolute" : "fixed";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: pos,
      inset: 0,
      zIndex: 1500
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: close,
    style: {
      position: "absolute",
      inset: 0,
      background: "var(--black-alpha-500)",
      animation: "chakra-fade-in var(--duration-slow)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      inset: 0,
      display: "flex",
      justifyContent: "center",
      alignItems: placement === "center" ? "center" : placement === "bottom" ? "flex-end" : "flex-start",
      pointerEvents: "none",
      padding: "0 16px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    role: "dialog",
    "aria-modal": "true",
    style: {
      pointerEvents: "auto",
      display: "flex",
      flexDirection: "column",
      position: "relative",
      width: "100%",
      maxWidth: MAXW[size] || MAXW.md,
      borderRadius: "var(--radius-l3)",
      ...__ds_scope.TEXT.sm,
      background: "var(--bg-panel)",
      boxShadow: "var(--shadow-lg)",
      color: "var(--fg)",
      margin: placement === "center" ? "auto" : "4rem auto",
      animation: "chakra-scale-in var(--duration-moderate), chakra-fade-in var(--duration-moderate)",
      ...style
    }
  }, (title || description) && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 8,
      padding: "24px 24px 16px"
    }
  }, title && /*#__PURE__*/React.createElement("div", {
    style: {
      ...__ds_scope.TEXT.lg,
      fontWeight: 600
    }
  }, title), description && /*#__PURE__*/React.createElement("div", {
    style: {
      color: "var(--fg-muted)"
    }
  }, description)), children && /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      padding: "8px 24px 24px"
    }
  }, children), footer && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "flex-end",
      gap: 12,
      padding: "8px 24px 16px"
    }
  }, footer), showClose && /*#__PURE__*/React.createElement("button", {
    type: "button",
    "aria-label": "Close",
    onClick: close,
    className: "chakra-focus-ring",
    style: {
      position: "absolute",
      top: 8,
      insetInlineEnd: 8,
      width: 36,
      height: 36,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      border: 0,
      background: "transparent",
      borderRadius: "var(--radius-l2)",
      color: "var(--gray-fg)",
      cursor: "pointer",
      "--icon-size": "16px"
    }
  }, /*#__PURE__*/React.createElement(__ds_scope.Icon, {
    name: "x"
  })))));
}
Object.assign(__ds_scope, { Dialog });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/overlays/Dialog.jsx", error: String((e && e.message) || e) }); }

// components/overlays/Menu.jsx
try { (() => {
// Static menu surface. Pair with any trigger; `items` = [{value,label,command,icon,disabled}] | {type:'separator'} | {type:'group',label}
function Menu({
  items = [],
  size = "md",
  variant = "subtle",
  colorPalette = "gray",
  onSelect,
  style
}) {
  const [hl, setHl] = React.useState(null);
  const sm = size === "sm";
  return /*#__PURE__*/React.createElement("div", {
    role: "menu",
    style: {
      ...__ds_scope.cp(colorPalette),
      minWidth: "8rem",
      padding: sm ? 4 : 6,
      background: "var(--bg-panel)",
      boxShadow: "var(--shadow-lg)",
      color: "var(--fg)",
      borderRadius: "var(--radius-l2)",
      overflow: "hidden",
      animation: "chakra-slide-from-top var(--duration-fast), chakra-fade-in var(--duration-fast)",
      ...style
    }
  }, items.map((it, i) => {
    if (it.type === "separator") return /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        height: 1,
        background: "var(--bg-muted)",
        margin: sm ? "4px -4px" : "4px -6px"
      }
    });
    if (it.type === "group") return /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        padding: "6px 8px",
        fontWeight: 600,
        ...__ds_scope.TEXT.sm
      }
    }, it.label);
    const on = hl === i && !it.disabled;
    const hlStyle = on ? variant === "solid" ? {
      background: "var(--cp-solid)",
      color: "var(--cp-contrast)"
    } : {
      background: __ds_scope.mix("var(--bg-emphasized)", 60)
    } : {};
    return /*#__PURE__*/React.createElement("div", {
      key: i,
      role: "menuitem",
      "aria-disabled": it.disabled || undefined,
      onMouseEnter: () => setHl(i),
      onMouseLeave: () => setHl(null),
      onClick: () => !it.disabled && onSelect && onSelect(it.value),
      style: {
        display: "flex",
        alignItems: "center",
        width: "100%",
        gap: sm ? 4 : 8,
        padding: sm ? "4px 6px" : "6px 8px",
        ...__ds_scope.TEXT[sm ? "xs" : "sm"],
        borderRadius: "var(--radius-l1)",
        cursor: it.disabled ? "not-allowed" : "pointer",
        userSelect: "none",
        opacity: it.disabled ? 0.5 : 1,
        color: it.danger ? "var(--fg-error)" : undefined,
        "--icon-size": "16px",
        ...hlStyle
      }
    }, it.icon, /*#__PURE__*/React.createElement("span", {
      style: {
        flex: 1
      }
    }, it.label), it.command && /*#__PURE__*/React.createElement("span", {
      style: {
        opacity: 0.6,
        ...__ds_scope.TEXT.xs,
        marginInlineStart: "auto",
        paddingInlineStart: 16,
        letterSpacing: "0.1em"
      }
    }, it.command));
  }));
}
Object.assign(__ds_scope, { Menu });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/overlays/Menu.jsx", error: String((e && e.message) || e) }); }

// components/overlays/Popover.jsx
try { (() => {
const PAD = {
  xs: 12,
  sm: 16,
  md: 20,
  lg: 24
};

// Static popover surface (no positioning engine). Place it under its trigger.
function Popover({
  title,
  children,
  footer,
  size = "md",
  width = "20rem",
  showArrow,
  style
}) {
  const p = PAD[size] || PAD.md;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      display: "flex",
      flexDirection: "column",
      ...__ds_scope.TEXT.sm,
      background: "var(--bg-panel)",
      boxShadow: "var(--shadow-lg)",
      width,
      borderRadius: "var(--radius-l3)",
      color: "var(--fg)",
      animation: "chakra-scale-in var(--duration-fast), chakra-fade-in var(--duration-fast)",
      ...style
    }
  }, showArrow && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      top: -6,
      left: 24,
      width: 12,
      height: 12,
      background: "var(--bg-panel)",
      transform: "rotate(45deg)",
      boxShadow: "-1px -1px 1px rgba(24,24,27,0.1)"
    }
  }), title && /*#__PURE__*/React.createElement("div", {
    style: {
      paddingInline: p,
      paddingTop: p,
      fontWeight: 600
    }
  }, title), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: p,
      flex: 1,
      position: "relative"
    }
  }, children), footer && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      paddingInline: p,
      paddingBottom: p
    }
  }, footer));
}
Object.assign(__ds_scope, { Popover });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/overlays/Popover.jsx", error: String((e && e.message) || e) }); }

// ui_kits/website/DocsChrome.jsx
try { (() => {
// Docs surface — components/docs/header.tsx, app/docs/sidebar.tsx, components/sidenav.tsx, page-header.tsx, toc.tsx, example.tsx, mdx/*
const SECONDARY = ["Get Started", "Components", "Charts", "Styling", "Theming"];
const SIDEBAR = {
  "Get Started": [{
    title: "Overview",
    items: ["Installation", "Migration", "CLI", "Figma", "Contributing"]
  }, {
    title: "AI for Agents",
    items: ["MCP Server", "LLMs.txt", "AI Skills"]
  }, {
    title: "Frameworks",
    items: ["Next.js (App)", "Next.js (Pages)", "Remix", "Storybook", "TanStack Router", "Vite"]
  }],
  Components: [{
    title: "Buttons",
    items: ["Button", "Close Button", "Icon Button"]
  }, {
    title: "Date and Time",
    items: [["Date Input", "new"], "Date Picker", "Calendar"]
  }, {
    title: "Forms",
    items: ["Checkbox", "Field", "Input", "Pin Input", "Radio", "Segmented Control", "Select (Native)", "Switch", "Slider", "Textarea"]
  }, {
    title: "Overlays",
    items: ["Dialog", "Menu", "Popover", "Tooltip"]
  }, {
    title: "Disclosure",
    items: ["Accordion", "Breadcrumb", "Tabs"]
  }, {
    title: "Feedback",
    items: ["Alert", "Empty State", "Progress", "Skeleton", "Spinner", "Status", "Toast"]
  }, {
    title: "Data Display",
    items: ["Avatar", "Badge", "Card", "Stat", "Table", "Tag"]
  }],
  Charts: [{
    title: "Overview",
    items: ["Installation", "useChart"]
  }, {
    title: "Charts",
    items: ["Area Chart", "Bar Chart", "Donut Chart", "Line Chart"]
  }],
  Styling: [{
    title: "Concepts",
    items: ["Overview", "Chakra Factory", "Responsive Design", "CSS Variables", "Dark Mode"]
  }, {
    title: "Compositions",
    items: ["Text Styles", "Layer Styles", "Focus Ring"]
  }],
  Theming: [{
    title: "Concepts",
    items: ["Overview", "Tokens", "Semantic Tokens", "Recipes", "Slot Recipes"]
  }, {
    title: "Design Tokens",
    items: ["Colors", "Radii", "Shadows", "Spacing", "Typography"]
  }]
};
const StatusBadge = ({
  children
}) => {
  const {
    Badge
  } = DS;
  return /*#__PURE__*/React.createElement(Badge, {
    size: "xs",
    variant: "solid",
    colorPalette: "teal",
    style: {
      textTransform: "capitalize",
      fontSize: "var(--font-size-xs)"
    }
  }, children);
};
function TopNavLink({
  children,
  current,
  tab,
  onClick
}) {
  const [h, setH] = React.useState(false);
  return /*#__PURE__*/React.createElement("a", {
    href: "#",
    onClick: e => {
      e.preventDefault();
      onClick && onClick();
    },
    onMouseEnter: () => setH(true),
    onMouseLeave: () => setH(false),
    "aria-current": current ? "page" : undefined,
    style: {
      fontSize: "var(--font-size-sm)",
      color: current || h ? "var(--fg)" : "var(--fg-muted)",
      fontWeight: current ? 500 : 400,
      textDecoration: "none",
      display: "inline-flex",
      alignItems: "center",
      gap: 4,
      ...(tab ? {
        padding: "8px 0",
        borderBottom: "2px solid",
        borderColor: current ? "var(--teal-solid)" : h ? "var(--border)" : "transparent",
        transition: "border-color 0.2s"
      } : {})
    }
  }, children);
}
function SearchButton() {
  const {
    Button,
    Icon,
    Kbd
  } = DS;
  return /*#__PURE__*/React.createElement(Button, {
    variant: "subtle",
    size: "sm",
    style: {
      width: 256,
      flexShrink: 1,
      color: "var(--fg-muted)",
      justifyContent: "flex-start"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "search",
    style: {
      marginInlineStart: -4
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      marginInlineStart: 4,
      fontWeight: 400,
      flex: 1,
      textAlign: "start"
    }
  }, "Search..."), /*#__PURE__*/React.createElement(Kbd, {
    variant: "outline",
    style: {
      background: "var(--bg)",
      fontSize: "0.8em",
      letterSpacing: "0.1em",
      marginInlineEnd: -4
    }
  }, "\u2318K"));
}
function VersionMenu() {
  const {
    Button,
    Icon,
    Menu
  } = DS;
  const [open, setOpen] = React.useState(false);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement(Button, {
    size: "sm",
    variant: "outline",
    style: {
      gap: 4,
      paddingInlineEnd: 8
    },
    onClick: () => setOpen(o => !o)
  }, "3.37.0", /*#__PURE__*/React.createElement(Icon, {
    name: "chevron-down"
  })), open && /*#__PURE__*/React.createElement(Menu, {
    style: {
      position: "absolute",
      top: 40,
      right: 0,
      width: 160,
      zIndex: 20
    },
    onSelect: () => setOpen(false),
    items: [{
      value: "v2",
      label: /*#__PURE__*/React.createElement("span", {
        style: {
          display: "flex"
        }
      }, /*#__PURE__*/React.createElement("b", {
        style: {
          fontWeight: 500,
          flex: 1
        }
      }, "v2"), /*#__PURE__*/React.createElement("span", {
        style: {
          color: "var(--fg-muted)"
        }
      }, "2.10.x"))
    }, {
      value: "v1",
      label: /*#__PURE__*/React.createElement("span", {
        style: {
          display: "flex"
        }
      }, /*#__PURE__*/React.createElement("b", {
        style: {
          fontWeight: 500,
          flex: 1
        }
      }, "v1"), /*#__PURE__*/React.createElement("span", {
        style: {
          color: "var(--fg-muted)"
        }
      }, "1.5.x"))
    }]
  }));
}
function DocsHeader({
  dark,
  onToggle,
  onNavigate,
  section,
  setSection
}) {
  const {
    Button,
    Icon
  } = DS;
  return /*#__PURE__*/React.createElement("header", {
    style: {
      background: "var(--bg)",
      position: "sticky",
      top: 0,
      display: "flex",
      justifyContent: "center",
      width: "100%",
      minHeight: 64,
      borderBottom: "1px solid var(--border-muted)",
      zIndex: 10
    }
  }, /*#__PURE__*/React.createElement(SiteContainer, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      padding: "8px 0"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 32,
      minHeight: 48
    }
  }, /*#__PURE__*/React.createElement("a", {
    href: "#",
    onClick: e => {
      e.preventDefault();
      onNavigate("home");
    }
  }, /*#__PURE__*/React.createElement(Logo, {
    dark: dark
  })), ["Docs", "Showcase", "Blog", "Guides"].map(t => /*#__PURE__*/React.createElement(TopNavLink, {
    key: t,
    current: t === "Docs"
  }, t))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      minHeight: 48
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "ghost",
    size: "sm"
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "heart",
    style: {
      color: "var(--red-500)"
    }
  }), "Sponsor"), /*#__PURE__*/React.createElement(VersionMenu, null), /*#__PURE__*/React.createElement(SearchButton, null), /*#__PURE__*/React.createElement(GithubButton, null), /*#__PURE__*/React.createElement(ColorModeButton, {
    dark: dark,
    onToggle: onToggle
  }))), /*#__PURE__*/React.createElement("nav", {
    "aria-label": "secondary navigation",
    style: {
      display: "flex",
      gap: 24
    }
  }, SECONDARY.map(t => /*#__PURE__*/React.createElement(TopNavLink, {
    key: t,
    tab: true,
    current: t === section,
    onClick: () => setSection(t)
  }, t)))));
}
function SideNavItem({
  title,
  status,
  current,
  onClick
}) {
  const [h, setH] = React.useState(false);
  const fill = current ? {
    background: "var(--teal-subtle)",
    color: "var(--teal-fg)",
    fontWeight: 500
  } : h ? {
    background: "var(--gray-subtle)",
    color: "var(--gray-fg)"
  } : {
    color: "var(--fg-muted)"
  };
  return /*#__PURE__*/React.createElement("a", {
    href: "#",
    onClick: e => {
      e.preventDefault();
      onClick();
    },
    onMouseEnter: () => setH(true),
    onMouseLeave: () => setH(false),
    "aria-current": current ? "page" : undefined,
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      padding: "6px 12px 6px 16px",
      borderRadius: "var(--radius-sm)",
      textDecoration: "none",
      ...fill
    }
  }, title, status && /*#__PURE__*/React.createElement(StatusBadge, null, status));
}
function SidebarStart({
  section,
  page,
  setPage
}) {
  return /*#__PURE__*/React.createElement("aside", {
    style: {
      position: "sticky",
      top: 105,
      alignSelf: "flex-start",
      paddingInlineEnd: 20,
      marginInlineStart: -12,
      padding: "32px 20px 32px 0",
      flexShrink: 0,
      height: "calc(100vh - 105px)",
      overflowY: "auto",
      width: "16rem",
      fontSize: "var(--font-size-sm)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 24
    }
  }, (SIDEBAR[section] || []).map(g => /*#__PURE__*/React.createElement("div", {
    key: g.title,
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      paddingInlineStart: 16,
      fontWeight: 600
    }
  }, g.title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 1
    }
  }, g.items.map(it => {
    const [t, s] = Array.isArray(it) ? it : [it];
    return /*#__PURE__*/React.createElement(SideNavItem, {
      key: t,
      title: t,
      status: s,
      current: t === page,
      onClick: () => setPage(t)
    });
  }))))));
}
const P = ({
  children
}) => /*#__PURE__*/React.createElement("p", {
  style: {
    color: "var(--fg-muted)",
    lineHeight: 1.75,
    margin: "1em 0"
  }
}, children);
const H2 = ({
  children,
  id
}) => /*#__PURE__*/React.createElement("h2", {
  id: id,
  style: {
    color: "var(--fg)",
    fontSize: "1.3em",
    letterSpacing: "-0.02em",
    margin: "1.6em 0 0.8em",
    lineHeight: "1.4em",
    fontWeight: 600
  }
}, children);
const H3 = ({
  children,
  id
}) => /*#__PURE__*/React.createElement("h3", {
  id: id,
  style: {
    color: "var(--fg)",
    fontSize: "1.2em",
    letterSpacing: "-0.01em",
    margin: "1.5em 0 0.4em",
    lineHeight: "1.5em",
    fontWeight: 600
  }
}, children);
const InlineCode = ({
  children
}) => /*#__PURE__*/React.createElement("code", {
  style: {
    fontSize: "0.8em",
    letterSpacing: "-0.01em",
    borderRadius: "var(--radius-md)",
    border: "0.5px solid var(--border)",
    background: "var(--bg-muted)",
    color: "var(--fg)",
    whiteSpace: "pre",
    padding: "0.1em 0.4em",
    fontFamily: "var(--font-mono)"
  }
}, children);
function Pre({
  code
}) {
  const {
    IconButton,
    Icon
  } = DS;
  const [copied, setCopied] = React.useState(false);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      margin: "1.6em 0"
    }
  }, /*#__PURE__*/React.createElement("pre", {
    style: {
      margin: 0,
      background: "var(--bg-muted)",
      boxShadow: "var(--shadow-inset)",
      borderRadius: "var(--radius-md)",
      fontSize: "0.9em",
      padding: "2em",
      overflowX: "auto",
      fontFamily: "var(--font-mono)",
      lineHeight: 1.7,
      color: "var(--fg)"
    }
  }, code), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 12,
      right: 14
    }
  }, /*#__PURE__*/React.createElement(IconButton, {
    "aria-label": "Copy",
    variant: "ghost",
    size: "sm",
    onClick: () => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: copied ? "check" : "copy"
  }))));
}
function ExampleTabs({
  preview,
  code
}) {
  const {
    Tabs
  } = DS;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: "2em",
      marginBottom: "4em"
    }
  }, /*#__PURE__*/React.createElement(Tabs, {
    variant: "subtle",
    size: "sm",
    listStyle: {
      marginBottom: 16,
      width: "100%",
      display: "flex"
    },
    items: [{
      value: "preview",
      label: "Preview",
      content: /*#__PURE__*/React.createElement("div", {
        style: {
          border: "1px solid var(--border)",
          borderRadius: "var(--radius-md)",
          padding: 40,
          marginTop: -12
        }
      }, preview)
    }, {
      value: "code",
      label: "Code",
      content: /*#__PURE__*/React.createElement("div", {
        style: {
          border: "1px solid var(--border)",
          borderRadius: "var(--radius-md)",
          overflow: "hidden",
          marginTop: -12
        }
      }, /*#__PURE__*/React.createElement("pre", {
        style: {
          margin: 0,
          padding: 24,
          fontFamily: "var(--font-mono)",
          fontSize: 13,
          lineHeight: 1.7,
          background: "var(--bg-muted)",
          maxHeight: 480,
          overflow: "auto"
        }
      }, code))
    }]
  }));
}
function PageHeader({
  title,
  description
}) {
  const {
    Badge,
    Icon
  } = DS;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16,
      paddingBottom: 16,
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      margin: 0,
      fontSize: "var(--font-size-3xl)",
      lineHeight: "var(--line-height-3xl)",
      fontWeight: 600,
      letterSpacing: "-0.025em"
    }
  }, title), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      color: "var(--fg-muted)"
    }
  }, description), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 24,
      flexWrap: "wrap"
    }
  }, ["Source", "Storybook", "Recipe", "Ark"].map(l => /*#__PURE__*/React.createElement("a", {
    key: l,
    href: "#",
    style: {
      fontWeight: 500,
      fontSize: "var(--font-size-sm)",
      color: "var(--fg-muted)",
      textDecoration: "underline",
      textUnderlineOffset: 3,
      textDecorationColor: "color-mix(in srgb, currentColor 20%, transparent)",
      display: "inline-flex",
      alignItems: "center",
      gap: 6
    }
  }, l, /*#__PURE__*/React.createElement(Icon, {
    name: "arrow-up-right",
    style: {
      width: "1em",
      height: "1em",
      color: "var(--fg-subtle)"
    }
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 8,
      alignSelf: "flex-start",
      padding: 8,
      borderRadius: "var(--radius-l1)",
      background: "var(--orange-subtle)",
      color: "var(--orange-fg)",
      boxShadow: "0 0 0px 1px var(--orange-muted)",
      margin: "12px 0 16px"
    }
  }, /*#__PURE__*/React.createElement(Badge, {
    variant: "solid",
    colorPalette: "orange"
  }, "AI Tip"), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: "var(--font-size-sm)",
      lineHeight: "var(--line-height-sm)",
      paddingInlineEnd: 12
    }
  }, "Want to skip the docs? Use our ", /*#__PURE__*/React.createElement("a", {
    href: "#",
    style: {
      color: "inherit",
      textDecoration: "underline",
      fontWeight: 500
    }
  }, "Agent Skills"))));
}
function Toc({
  items,
  active
}) {
  return /*#__PURE__*/React.createElement("aside", {
    style: {
      position: "sticky",
      top: 105,
      alignSelf: "flex-start",
      padding: "32px 8px",
      flexShrink: 0,
      width: "16rem"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16,
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement("nav", {
    style: {
      fontSize: "var(--font-size-sm)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontWeight: 600
    }
  }, "On this page"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 8,
      marginTop: 12
    }
  }, items.map(([t, d]) => /*#__PURE__*/React.createElement("a", {
    key: t,
    href: "#",
    style: {
      fontSize: "var(--font-size-sm)",
      color: t === active ? "var(--fg)" : "var(--fg-muted)",
      fontWeight: t === active ? 500 : 400,
      marginInlineStart: `${d}rem`,
      textDecoration: "none"
    }
  }, t)))), /*#__PURE__*/React.createElement("div", {
    style: {
      borderTop: "1px solid var(--border)",
      paddingTop: 16,
      display: "flex",
      flexDirection: "column",
      gap: 8,
      width: "100%",
      fontSize: "var(--font-size-sm)"
    }
  }, /*#__PURE__*/React.createElement("a", {
    href: "#",
    style: {
      color: "var(--fg-muted)",
      textDecoration: "none"
    }
  }, "Edit this page \u2197"), /*#__PURE__*/React.createElement("a", {
    href: "#",
    style: {
      color: "var(--fg-muted)",
      textDecoration: "none"
    }
  }, "Scroll to top \u2191"))));
}
function Pagination({
  prev,
  next,
  onGo
}) {
  const {
    Icon
  } = DS;
  const item = (label, title, end) => /*#__PURE__*/React.createElement("a", {
    href: "#",
    onClick: e => {
      e.preventDefault();
      onGo(title);
    },
    style: {
      flex: 1,
      border: "1px solid var(--border)",
      borderRadius: "var(--radius-md)",
      padding: 16,
      textDecoration: "none",
      color: "var(--fg)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 4,
      textAlign: end ? "end" : "start",
      fontSize: "var(--font-size-sm)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)"
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      justifyContent: end ? "flex-end" : "flex-start",
      alignItems: "center",
      gap: 8,
      fontWeight: 500
    }
  }, !end && /*#__PURE__*/React.createElement(Icon, {
    name: "chevron-left",
    style: {
      width: 16,
      height: 16
    }
  }), title, end && /*#__PURE__*/React.createElement(Icon, {
    name: "chevron-right",
    style: {
      width: 16,
      height: 16
    }
  }))));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      marginTop: 48
    }
  }, prev ? item("Previous", prev) : /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), next ? item("Next", next, true) : /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }));
}
Object.assign(window, {
  DocsHeader,
  SidebarStart,
  PageHeader,
  ExampleTabs,
  Pre,
  P,
  H2,
  H3,
  InlineCode,
  Toc,
  Pagination,
  SIDEBAR
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/website/DocsChrome.jsx", error: String((e && e.message) || e) }); }

// ui_kits/website/DocsPage.jsx
try { (() => {
// Docs content for a handful of component pages (abbreviated MDX)
function docContent(page) {
  const {
    Button,
    IconButton,
    Icon,
    Badge,
    Switch,
    Tabs,
    Alert,
    Avatar,
    Checkbox,
    Input,
    Field,
    Slider,
    Tooltip,
    Card,
    Stat,
    Progress,
    Tag
  } = DS;
  const pages = {
    Button: {
      description: "Used to trigger an action or event",
      preview: /*#__PURE__*/React.createElement(Button, null, "Button"),
      code: `import { Button } from "@chakra-ui/react"\n\nexport const ButtonBasic = () => {\n  return <Button>Button</Button>\n}`,
      sections: [["Sizes", "Use the size prop to change the size of the button.", /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          gap: 16,
          alignItems: "center",
          flexWrap: "wrap"
        }
      }, ["xs", "sm", "md", "lg", "xl"].map(s => /*#__PURE__*/React.createElement(Button, {
        key: s,
        size: s,
        variant: "outline"
      }, "Button (", s, ")")))], ["Variants", "Use the variant prop to change the visual style of the Button.", /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          gap: 16,
          flexWrap: "wrap"
        }
      }, ["solid", "subtle", "surface", "outline", "ghost", "plain"].map(v => /*#__PURE__*/React.createElement(Button, {
        key: v,
        variant: v
      }, v[0].toUpperCase() + v.slice(1))))], ["Icon", "Use icons within a button.", /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          gap: 16
        }
      }, /*#__PURE__*/React.createElement(Button, {
        colorPalette: "teal"
      }, /*#__PURE__*/React.createElement(Icon, {
        name: "mail"
      }), " Email"), /*#__PURE__*/React.createElement(Button, {
        colorPalette: "teal",
        variant: "outline"
      }, "Call us ", /*#__PURE__*/React.createElement(Icon, {
        name: "arrow-right"
      })))], ["Loading", "Pass the loading and loadingText props to the Button component to show a loading spinner and add a loading text.", /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          gap: 16
        }
      }, /*#__PURE__*/React.createElement(Button, {
        loading: true
      }, "Click me"), /*#__PURE__*/React.createElement(Button, {
        loading: true,
        loadingText: "Saving...",
        variant: "outline"
      }, "Click me"))]]
    },
    Badge: {
      description: "Used to highlight an item's status for quick recognition.",
      preview: /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          gap: 8
        }
      }, /*#__PURE__*/React.createElement(Badge, null, "Default"), /*#__PURE__*/React.createElement(Badge, {
        colorPalette: "green"
      }, "Success"), /*#__PURE__*/React.createElement(Badge, {
        colorPalette: "red"
      }, "Removed"), /*#__PURE__*/React.createElement(Badge, {
        colorPalette: "purple"
      }, "New")),
      code: `<HStack>\n  <Badge>Default</Badge>\n  <Badge colorPalette="green">Success</Badge>\n  <Badge colorPalette="red">Removed</Badge>\n  <Badge colorPalette="purple">New</Badge>\n</HStack>`,
      sections: [["Variants", "Use the variant prop to change the visual style.", /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          gap: 8
        }
      }, ["outline", "solid", "subtle", "surface"].map(v => /*#__PURE__*/React.createElement(Badge, {
        key: v,
        variant: v
      }, v)))]]
    },
    Switch: {
      description: "Used to capture a binary state",
      preview: /*#__PURE__*/React.createElement(Switch, null, "Activate Chakra"),
      code: `<Switch.Root>\n  <Switch.HiddenInput />\n  <Switch.Control />\n  <Switch.Label>Activate Chakra</Switch.Label>\n</Switch.Root>`,
      sections: [["Sizes", "Pass the size prop to change the size of the switch component.", /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          gap: 24,
          alignItems: "center"
        }
      }, ["xs", "sm", "md", "lg"].map(s => /*#__PURE__*/React.createElement(Switch, {
        key: s,
        size: s,
        defaultChecked: true
      })))], ["Variants", "Pass the variant prop to change the visual style of the switch.", /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          gap: 24
        }
      }, /*#__PURE__*/React.createElement(Switch, {
        variant: "raised",
        defaultChecked: true
      }, "Raised"), /*#__PURE__*/React.createElement(Switch, {
        variant: "solid",
        defaultChecked: true
      }, "Solid"))]]
    },
    Tabs: {
      description: "Used to display content in a tabbed interface",
      preview: /*#__PURE__*/React.createElement(Tabs, {
        items: [{
          value: "members",
          label: "Members",
          icon: /*#__PURE__*/React.createElement(Icon, {
            name: "user"
          }),
          content: "Manage your team members"
        }, {
          value: "projects",
          label: "Projects",
          icon: /*#__PURE__*/React.createElement(Icon, {
            name: "box"
          }),
          content: "Manage your projects"
        }, {
          value: "tasks",
          label: "Settings",
          icon: /*#__PURE__*/React.createElement(Icon, {
            name: "settings"
          }),
          content: "Manage your tasks for freelancers"
        }]
      }),
      code: `<Tabs.Root defaultValue="members">\n  <Tabs.List>\n    <Tabs.Trigger value="members"><LuUser /> Members</Tabs.Trigger>\n    ...`,
      sections: [["Variants", "Use the variant prop to change the visual style of the tabs.", /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          flexDirection: "column",
          gap: 24
        }
      }, ["line", "subtle", "enclosed", "outline"].map(v => /*#__PURE__*/React.createElement(Tabs, {
        key: v,
        variant: v,
        items: [{
          value: "a",
          label: "Members"
        }, {
          value: "b",
          label: "Projects"
        }]
      })))]]
    },
    Alert: {
      description: "Used to communicate a state that affects a system, feature or page.",
      preview: /*#__PURE__*/React.createElement(Alert, {
        status: "info",
        title: "This is the alert title"
      }),
      code: `<Alert.Root status="info">\n  <Alert.Indicator />\n  <Alert.Title>This is the alert title</Alert.Title>\n</Alert.Root>`,
      sections: [["Status", "Change the status of the alerts by passing the status prop.", /*#__PURE__*/React.createElement("div", {
        style: {
          display: "flex",
          flexDirection: "column",
          gap: 8
        }
      }, ["error", "info", "warning", "success", "neutral"].map(s => /*#__PURE__*/React.createElement(Alert, {
        key: s,
        status: s,
        title: "There was an error processing your request"
      })))]]
    }
  };
  return pages[page] || {
    description: "Component documentation.",
    preview: /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        gap: 16,
        alignItems: "center",
        flexWrap: "wrap"
      }
    }, /*#__PURE__*/React.createElement(Avatar, {
      name: "Segun Adebayo"
    }), /*#__PURE__*/React.createElement(Checkbox, {
      defaultChecked: true
    }, "Accept terms"), /*#__PURE__*/React.createElement(Tag, null, "Tag"), /*#__PURE__*/React.createElement(Tooltip, {
      content: "Tooltip"
    }, /*#__PURE__*/React.createElement(Button, {
      size: "sm",
      variant: "outline"
    }, "Hover"))),
    code: `import { ${page.replace(/[^A-Za-z]/g, "")} } from "@chakra-ui/react"`,
    sections: []
  };
}
function DocsPage({
  dark,
  onToggle,
  onNavigate
}) {
  const [section, setSectionRaw] = React.useState("Components");
  const [page, setPage] = React.useState("Button");
  const setSection = s => {
    setSectionRaw(s);
    setPage(SIDEBAR[s][0].items[0]);
  };
  const flat = (SIDEBAR[section] || []).flatMap(g => g.items.map(i => Array.isArray(i) ? i[0] : i));
  const idx = flat.indexOf(page);
  const c = docContent(page);
  const toc = [["Usage", 0], ["Examples", 0], ...c.sections.map(([t]) => [t, 1]), ["Props", 0]];
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(DocsHeader, {
    dark: dark,
    onToggle: onToggle,
    onNavigate: onNavigate,
    section: section,
    setSection: setSection
  }), /*#__PURE__*/React.createElement("main", null, /*#__PURE__*/React.createElement(SiteContainer, {
    style: {
      display: "flex"
    }
  }, /*#__PURE__*/React.createElement(SidebarStart, {
    section: section,
    page: page,
    setPage: setPage
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      width: "100%",
      minWidth: 0,
      padding: "40px 48px 64px"
    }
  }, /*#__PURE__*/React.createElement(PageHeader, {
    title: page,
    description: c.description
  }), /*#__PURE__*/React.createElement(ExampleTabs, {
    key: page,
    preview: c.preview,
    code: c.code
  }), /*#__PURE__*/React.createElement(H2, null, "Usage"), /*#__PURE__*/React.createElement(Pre, {
    code: `import { ${page.replace(/[^A-Za-z]/g, "")} } from "@chakra-ui/react"`
  }), /*#__PURE__*/React.createElement(H2, null, "Examples"), c.sections.map(([t, d, prev]) => /*#__PURE__*/React.createElement("div", {
    key: t
  }, /*#__PURE__*/React.createElement(H3, null, t), /*#__PURE__*/React.createElement(P, null, d), /*#__PURE__*/React.createElement(ExampleTabs, {
    preview: prev,
    code: `// ${t.toLowerCase()} example`
  }))), /*#__PURE__*/React.createElement(H2, null, "Props"), /*#__PURE__*/React.createElement(P, null, "These props can be passed to the root component. ", /*#__PURE__*/React.createElement(InlineCode, null, "colorPalette"), " defaults to ", /*#__PURE__*/React.createElement(InlineCode, null, "\"gray\""), "."), /*#__PURE__*/React.createElement(Pagination, {
    prev: flat[idx - 1],
    next: flat[idx + 1],
    onGo: setPage
  })), /*#__PURE__*/React.createElement(Toc, {
    items: toc,
    active: "Usage"
  }))));
}
Object.assign(window, {
  DocsPage
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/website/DocsPage.jsx", error: String((e && e.message) || e) }); }

// ui_kits/website/HomePage.jsx
try { (() => {
// Marketing homepage — apps/www/app/page.tsx + components/site/*.section.tsx
const NAV_LINKS = ["Docs", "Showcase", "Spotlight", "Blog", "Guides"];
const frameBorder = dark => dark ? "#001B18" : "var(--border)";
function HomeHeader({
  dark,
  onToggle,
  onNavigate
}) {
  return /*#__PURE__*/React.createElement(SiteContainer, {
    style: {
      marginTop: 8
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("a", {
    href: "#",
    onClick: e => {
      e.preventDefault();
      onNavigate("home");
    },
    "aria-label": "Chakra UI, Back to homepage"
  }, /*#__PURE__*/React.createElement(Logo, {
    dark: dark
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement("nav", {
    "aria-label": "primary navigation",
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      minHeight: 48
    }
  }, NAV_LINKS.map(t => /*#__PURE__*/React.createElement("a", {
    key: t,
    href: "#",
    onClick: e => {
      e.preventDefault();
      if (t === "Docs") onNavigate("docs");
    },
    style: {
      display: "flex",
      alignItems: "center",
      minHeight: 32,
      padding: "0 12px",
      borderRadius: "var(--radius-md)",
      fontWeight: 500,
      fontSize: "var(--font-size-sm)",
      lineHeight: "var(--line-height-sm)",
      color: "var(--fg)",
      textDecoration: "none"
    }
  }, t)), /*#__PURE__*/React.createElement(GithubButton, null)), /*#__PURE__*/React.createElement(ColorModeButton, {
    dark: dark,
    onToggle: onToggle
  }))));
}
function DemoFrame({
  dark,
  label,
  children,
  contentStyle
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      border: `0.5px solid ${frameBorder(dark)}`,
      width: 400,
      flexShrink: 0,
      position: "relative",
      userSelect: "none",
      paddingBottom: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      margin: "0 auto",
      maxWidth: 200,
      minHeight: 240,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      ...contentStyle
    }
  }, children), /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--font-size-sm)",
      lineHeight: "var(--line-height-sm)",
      color: "var(--fg-muted)",
      fontFamily: "var(--font-mono)",
      textAlign: "center"
    }
  }, label));
}
function ComponentDemos({
  dark
}) {
  const {
    Slider,
    PinInput,
    Tabs,
    Button,
    Menu,
    Switch
  } = DS;
  const [menuOpen, setMenuOpen] = React.useState(false);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 16,
      overflowX: "auto",
      paddingInlineStart: "max(24px, calc(50% - 86rem / 2))",
      paddingInlineEnd: 24,
      scrollbarWidth: "none"
    }
  }, /*#__PURE__*/React.createElement(DemoFrame, {
    dark: dark,
    label: "Slider"
  }, /*#__PURE__*/React.createElement(Slider, {
    colorPalette: "teal",
    defaultValue: 40,
    size: "lg"
  })), /*#__PURE__*/React.createElement(DemoFrame, {
    dark: dark,
    label: "Pin Input"
  }, /*#__PURE__*/React.createElement(PinInput, {
    size: "lg",
    count: 4
  })), /*#__PURE__*/React.createElement(DemoFrame, {
    dark: dark,
    label: "Tabs",
    contentStyle: {
      maxWidth: "80%"
    }
  }, /*#__PURE__*/React.createElement(Tabs, {
    fitted: true,
    variant: "enclosed",
    colorPalette: "teal",
    style: {
      width: "100%"
    },
    items: [{
      value: "Chakra",
      label: "Chakra",
      content: /*#__PURE__*/React.createElement("div", {
        style: {
          padding: 8,
          fontSize: "var(--font-size-sm)"
        }
      }, "Chakra UI is a component library for building web applications.")
    }, {
      value: "Ark",
      label: "Ark",
      content: /*#__PURE__*/React.createElement("div", {
        style: {
          padding: 8,
          fontSize: "var(--font-size-sm)"
        }
      }, "Ark UI is a headless library for building reusable, scalable design systems")
    }, {
      value: "Zag",
      label: "Zag",
      content: /*#__PURE__*/React.createElement("div", {
        style: {
          padding: 8,
          fontSize: "var(--font-size-sm)"
        }
      }, "Zag.js provides a set of UI components powered by Finite State Machines")
    }]
  })), /*#__PURE__*/React.createElement(DemoFrame, {
    dark: dark,
    label: "Menu"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "outline",
    size: "sm",
    onClick: () => setMenuOpen(o => !o)
  }, "Open Menu"), menuOpen && /*#__PURE__*/React.createElement(Menu, {
    style: {
      position: "absolute",
      top: 44,
      left: "50%",
      transform: "translateX(-50%)",
      width: 180,
      zIndex: 5
    },
    onSelect: () => setMenuOpen(false),
    items: [{
      value: "new-txt",
      label: "New Text File"
    }, {
      value: "new-file",
      label: "New File..."
    }, {
      value: "new-win",
      label: "New Window"
    }]
  }))), /*#__PURE__*/React.createElement(DemoFrame, {
    dark: dark,
    label: "Switch"
  }, /*#__PURE__*/React.createElement(Switch, {
    size: "lg",
    colorPalette: "teal"
  })));
}
function HeroSection({
  dark,
  onNavigate
}) {
  const {
    Button,
    Icon
  } = DS;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      paddingTop: 64,
      paddingBottom: 80
    }
  }, /*#__PURE__*/React.createElement(SiteContainer, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 40,
      marginBottom: 80
    }
  }, /*#__PURE__*/React.createElement("a", {
    href: "#",
    style: {
      alignSelf: "flex-start",
      display: "inline-flex",
      alignItems: "center",
      gap: 10,
      padding: "8px 16px",
      fontWeight: 500,
      fontSize: "var(--font-size-sm)",
      lineHeight: "var(--line-height-sm)",
      background: "color-mix(in srgb, var(--teal-500) 10%, transparent)",
      color: "var(--teal-fg)",
      borderRadius: "var(--radius-l2)",
      textDecoration: "none",
      "--icon-size": "16px"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "party-popper"
  }), "Meet DateInput", /*#__PURE__*/React.createElement(Icon, {
    name: "arrow-right"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 20,
      maxWidth: "48rem",
      padding: "0 6px"
    }
  }, /*#__PURE__*/React.createElement(HighlightHeading, {
    query: "with speed"
  }, "Chakra UI is a component system for building products with speed"), /*#__PURE__*/React.createElement(Subheading, null, "Accessible React components for building high-quality web apps and design systems. ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg)"
    }
  }, "Works with Next.js RSC"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 12,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(Button, {
    size: "xl",
    colorPalette: "teal",
    style: {
      minWidth: 180
    },
    onClick: () => onNavigate("docs")
  }, "Start Building"), /*#__PURE__*/React.createElement("pre", {
    style: {
      margin: 0,
      minHeight: 48,
      display: "flex",
      alignItems: "center",
      gap: 10,
      boxShadow: "var(--shadow-inset)",
      background: "color-mix(in srgb, var(--bg-subtle) 50%, transparent)",
      color: dark ? "var(--teal-400)" : "var(--teal-600)",
      fontSize: "var(--font-size-sm)",
      lineHeight: "var(--line-height-sm)",
      fontWeight: 600,
      paddingInlineStart: 16,
      paddingInlineEnd: 24,
      borderRadius: "var(--radius-l2)",
      fontFamily: "var(--font-mono)"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "terminal",
    style: {
      width: 16,
      height: 16
    }
  }), "npm i @chakra-ui/react")))), /*#__PURE__*/React.createElement(ComponentDemos, {
    dark: dark
  }));
}
function BlitzHeading({
  children,
  dark
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 16,
      color: dark ? "var(--teal-500)" : "var(--teal-600)",
      margin: "16px 0"
    }
  }, /*#__PURE__*/React.createElement(BlitzIcon, {
    style: {
      width: 13,
      height: 18
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      fontWeight: 600
    }
  }, children));
}
const DS_ITEMS = [{
  icon: "box",
  value: "tokens",
  label: "Tokens",
  description: "Streamline design decisions with semantic tokens",
  code: `import { defineConfig } from "@chakra-ui/react"\n\nconst config = defineConfig({\n  theme: {\n    tokens: {\n      colors: {\n        brand: {\n          50: { value: "#e6f2ff" },\n          100: { value: "#e6f2ff" },\n          200: { value: "#bfdeff" },\n          500: { value: "#0080ff" },\n        },\n      },\n    },\n  },\n})`
}, {
  icon: "type",
  value: "typography",
  label: "Typography",
  description: "Customise your font related properties in one place",
  code: `const config = defineConfig({\n  theme: {\n    tokens: {\n      fonts: {\n        heading: { value: "Figtree, sans-serif" },\n        body: { value: "Figtree, sans-serif" },\n      },\n    },\n  },\n})`
}, {
  icon: "paint-bucket",
  value: "recipes",
  label: "Recipes",
  description: "Design components variants with ease",
  code: `const buttonRecipe = defineRecipe({\n  base: { display: "flex" },\n  variants: {\n    visual: {\n      solid: { bg: "red.200", color: "white" },\n      outline: { borderWidth: "1px" },\n    },\n  },\n})`
}];
function DesignSystemSection({
  dark
}) {
  const {
    Icon,
    Tabs
  } = DS;
  return /*#__PURE__*/React.createElement("section", {
    style: {
      padding: "80px 0"
    }
  }, /*#__PURE__*/React.createElement(SiteContainer, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 80,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      alignItems: "flex-start",
      gap: 16,
      flex: 1,
      maxWidth: 576,
      minWidth: 320
    }
  }, /*#__PURE__*/React.createElement(BlitzHeading, {
    dark: dark
  }, "Design System"), /*#__PURE__*/React.createElement(HighlightHeading, {
    as: "h2",
    query: "Chakra UI"
  }, "Build your design system on top of Chakra UI"), /*#__PURE__*/React.createElement(Subheading, {
    size: "lg"
  }, "Spend less time writing UI code and more time building a great experience for your customers."), /*#__PURE__*/React.createElement("ul", {
    style: {
      listStyle: "none",
      padding: 0,
      margin: "16px 0 0",
      display: "flex",
      flexDirection: "column",
      gap: 24
    }
  }, DS_ITEMS.map(it => /*#__PURE__*/React.createElement("li", {
    key: it.value,
    style: {
      display: "flex",
      gap: 8,
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: it.icon,
    style: {
      width: 16,
      height: 16,
      marginTop: 4
    }
  }), /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0
    }
  }, it.label, ". ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)"
    }
  }, it.description)))))), /*#__PURE__*/React.createElement("div", {
    className: "dark",
    style: {
      flex: 1,
      minWidth: 320,
      background: "transparent"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      borderRadius: "var(--radius-lg)",
      padding: 8,
      boxShadow: "var(--shadow-inset)",
      background: "var(--bg)"
    }
  }, /*#__PURE__*/React.createElement(Tabs, {
    variant: "subtle",
    listStyle: {
      padding: 8
    },
    items: DS_ITEMS.map(it => ({
      value: it.value,
      label: it.label,
      icon: /*#__PURE__*/React.createElement(Icon, {
        name: it.icon
      }),
      content: /*#__PURE__*/React.createElement("pre", {
        style: {
          margin: 0,
          padding: 16,
          fontFamily: "var(--font-mono)",
          fontSize: 13,
          lineHeight: 1.7,
          color: "var(--fg)",
          overflow: "auto"
        }
      }, it.code)
    }))
  }))))));
}
function StatSection({
  dark
}) {
  const {
    Icon
  } = DS;
  const stats = [["github", "github stars", "40k+"], ["box", "downloads  / month", "2.5M+"], ["user", "discord members", "20k+"]];
  return /*#__PURE__*/React.createElement("section", {
    style: {
      padding: "80px 0"
    }
  }, /*#__PURE__*/React.createElement(SiteContainer, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 80,
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 56
    }
  }, /*#__PURE__*/React.createElement(HighlightHeading, {
    as: "h2",
    query: "By developers",
    style: {
      maxWidth: 500,
      textAlign: "center"
    }
  }, "Built for developers By developers"), /*#__PURE__*/React.createElement("h3", {
    style: {
      margin: 0,
      textAlign: "center",
      fontWeight: 500,
      fontSize: "var(--font-size-xl)",
      lineHeight: "var(--line-height-xl)"
    }
  }, "Built for modern product teams.", /*#__PURE__*/React.createElement("br", null), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)"
    }
  }, "From next-gen startups to established enterprises."))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      width: "100%",
      maxWidth: "72rem",
      flexWrap: "wrap"
    }
  }, stats.map(([ic, title, val]) => /*#__PURE__*/React.createElement("div", {
    key: title,
    style: {
      flex: 1,
      minWidth: 240,
      marginInlineStart: -1,
      border: `1px solid ${frameBorder(dark)}`,
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 24,
      padding: "40px 0 32px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontWeight: 500,
      color: dark ? "var(--teal-500)" : "var(--teal-600)",
      fontSize: "var(--font-size-7xl)",
      lineHeight: "var(--line-height-7xl)",
      letterSpacing: "-0.025em"
    }
  }, val), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(Icon, {
    name: ic,
    style: {
      width: 16,
      height: 16,
      color: dark ? "var(--teal-500)" : "var(--teal-600)"
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)",
      fontWeight: 500
    }
  }, title))))))));
}
const TESTIMONIALS = [{
  name: "Guillermo Rauch",
  role: "CEO / Vercel",
  img: "rauchg",
  content: "Chakra UI is glorious. Dark mode support looks amazing and it is 100% built-in. I love the consistent use of focus styling and the subtle animation. Great care for accessibility throughout. It is a guiding principle of the design system."
}, {
  name: "Aayush Iyer",
  role: "Engineer / Twillio",
  img: "aayush",
  content: "Chakra is a fantastic component library that helps shape and accelerate the work we're doing with Twilio Paste. \n\nThank you @thesegunadebayo!"
}, {
  name: "Colm Tuite",
  role: "CEO / Modulz",
  img: "colmtuite",
  content: "Awesome new open-source component library from @thesegunadebayo.\n\nReally impressive stuff!"
}, {
  name: "Echobind Engineering",
  role: "Echobind",
  img: "echobind",
  content: "Chakra UI has become part of our default stack for React apps, Chakra saves our team tons of time, is well designed and documented, has solid accessibility defaults, and looks great out of the box."
}];
function TestimonialSection({
  dark
}) {
  return /*#__PURE__*/React.createElement("section", {
    style: {
      padding: "80px 0"
    }
  }, /*#__PURE__*/React.createElement(SiteContainer, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 56
    }
  }, /*#__PURE__*/React.createElement(HighlightHeading, {
    as: "h2",
    size: "3xl",
    query: "love Chakra"
  }, "Top-tier teams use and love Chakra"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(min(100%, 420px), 1fr))",
      gap: 20,
      width: "100%"
    }
  }, TESTIMONIALS.map(t => /*#__PURE__*/React.createElement("a", {
    key: t.name,
    href: "#",
    style: {
      display: "flex",
      flexDirection: "column",
      justifyContent: "space-between",
      gap: 40,
      padding: 48,
      border: `1px solid ${frameBorder(dark)}`,
      textDecoration: "none",
      color: "var(--fg)"
    }
  }, /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      color: "var(--fg-muted)",
      whiteSpace: "pre-wrap"
    }
  }, t.content), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: "var(--font-size-sm)",
      display: "flex",
      flexDirection: "column",
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("span", null, t.name), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)"
    }
  }, t.role)), /*#__PURE__*/React.createElement("img", {
    src: `../../assets/avatars/${t.img}.jpg`,
    alt: t.name,
    width: "48",
    height: "48",
    style: {
      borderRadius: 10
    }
  }))))))));
}
const LINK_TREE = [{
  title: "Documentation",
  items: ["Components", "Theming", "Styling", "Get Started"]
}, {
  title: "Projects",
  items: ["Chakra UI Pro", "FigPilot", "Zag.js", "Ark UI"]
}, {
  title: "Community",
  items: ["Discord", "Twitter", "GitHub"]
}];
function FooterSection({
  dark
}) {
  return /*#__PURE__*/React.createElement("footer", {
    role: "contentinfo"
  }, /*#__PURE__*/React.createElement(SiteContainer, {
    style: {
      padding: "32px 32px",
      fontSize: "var(--font-size-sm)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "flex-start",
      gap: 32,
      flexWrap: "wrap-reverse"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 8,
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement(Logo, {
    dark: dark
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)"
    }
  }, "Project by Chakra Systems \xA9 2026"), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)",
      fontWeight: 500,
      margin: "24px 0"
    }
  }, "Deployed on Vercel"), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--fg-muted)"
    }
  }, "Maintained by ", /*#__PURE__*/React.createElement("a", {
    href: "#",
    style: {
      color: "var(--fg)",
      textDecoration: "underline"
    }
  }, "Sage"), " and contributors")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
      gap: 40,
      width: "100%",
      maxWidth: "42rem",
      paddingBottom: 80
    }
  }, LINK_TREE.map(c => /*#__PURE__*/React.createElement("div", {
    key: c.title,
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("span", null, c.title), c.items.map(i => /*#__PURE__*/React.createElement("a", {
    key: i,
    href: "#",
    style: {
      color: "var(--fg-muted)",
      textDecoration: "none"
    }
  }, i))))))));
}
function AmbientLights() {
  const blob = s => /*#__PURE__*/React.createElement("div", {
    style: {
      width: 1000,
      height: 800,
      position: "absolute",
      opacity: 0.05,
      pointerEvents: "none",
      flexShrink: 0,
      backgroundImage: "radial-gradient(#5EEAD4 0%, rgba(94, 234, 212, 0) 60%)",
      zIndex: 0,
      ...s
    }
  });
  return /*#__PURE__*/React.createElement(React.Fragment, null, blob({
    top: -50,
    left: "50%",
    transform: "translateX(-50%)"
  }), blob({
    left: "-30%",
    top: "40%"
  }), blob({
    height: 2000,
    width: 1200,
    left: "40%",
    top: 1200,
    transform: "translateX(-40%)"
  }));
}
function HomePage({
  dark,
  onToggle,
  onNavigate
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      display: "flex",
      flexDirection: "column",
      gap: 32,
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement(AmbientLights, null), /*#__PURE__*/React.createElement(BlitzIcon, {
    style: {
      position: "absolute",
      width: 245,
      height: 342,
      top: 58,
      right: 67,
      color: "var(--fg-inverted)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement(HomeHeader, {
    dark: dark,
    onToggle: onToggle,
    onNavigate: onNavigate
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement(HeroSection, {
    dark: dark,
    onNavigate: onNavigate
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement(DesignSystemSection, {
    dark: dark
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement(StatSection, {
    dark: dark
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement(TestimonialSection, {
    dark: dark
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement(FooterSection, {
    dark: dark
  })));
}
Object.assign(window, {
  HomePage
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/website/HomePage.jsx", error: String((e && e.message) || e) }); }

// ui_kits/website/Shared.jsx
try { (() => {
// Shared bits for the chakra-ui.com recreation
const DS = window.ChakraUIDesignSystem_349c6d;
function Logo({
  dark,
  height = 36
}) {
  return /*#__PURE__*/React.createElement("img", {
    src: dark ? "../../assets/logo-white.svg" : "../../assets/logo.svg",
    alt: "Chakra UI",
    style: {
      height,
      display: "block"
    }
  });
}
function BlitzIcon({
  style
}) {
  return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 26 36",
    fill: "none",
    style: style,
    "aria-hidden": "true"
  }, /*#__PURE__*/React.createElement("path", {
    d: "M0.439466 19.4621L19.1409 0.368881C19.4902 0.0121879 20.0525 0.446251 19.8166 0.890609L12.8561 14.0056C12.7007 14.298 12.9067 14.6549 13.2308 14.6549H25.2597C25.6473 14.6549 25.8343 15.1428 25.551 15.4147L4.47199 35.639C4.09405 36.0016 3.53148 35.4906 3.83492 35.0604L13.8133 20.9092C14.0178 20.6191 13.8163 20.2126 13.4676 20.2126H0.74036C0.359231 20.2126 0.169039 19.7382 0.439466 19.4621Z",
    fill: "currentColor"
  }));
}

// HighlightHeading — teal tinted block with 2px end border on the query phrase (components/site/typography.tsx)
function HighlightHeading({
  as = "h1",
  children,
  query,
  size = "5xl",
  style
}) {
  const Tag = as;
  const toText = n => typeof n === "string" || typeof n === "number" ? String(n) : Array.isArray(n) ? n.map(toText).join("") : n && n.props ? toText(n.props.children) : "";
  const text = toText(children);
  const i = text.toLowerCase().indexOf(query.toLowerCase());
  const pre = i >= 0 ? text.slice(0, i) : text;
  const hit = i >= 0 ? text.slice(i, i + query.length) : "";
  const post = i >= 0 ? text.slice(i + query.length) : "";
  return /*#__PURE__*/React.createElement(Tag, {
    style: {
      margin: 0,
      fontWeight: 600,
      fontSize: `var(--font-size-${size})`,
      lineHeight: `var(--line-height-${size})`,
      letterSpacing: "-0.025em",
      textWrap: "balance",
      ...style
    }
  }, pre, hit && /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--hl-color)",
      position: "relative",
      padding: "0 8px",
      display: "inline-block"
    }
  }, /*#__PURE__*/React.createElement("span", {
    "aria-hidden": "true",
    style: {
      position: "absolute",
      inset: 0,
      bottom: -2,
      background: "color-mix(in srgb, var(--teal-500) 10%, transparent)",
      borderTopLeftRadius: 6,
      borderBottomLeftRadius: 6,
      borderInlineEnd: "2px solid currentColor",
      pointerEvents: "none"
    }
  }), hit), post);
}
function Subheading({
  children,
  size = "xl",
  style
}) {
  return /*#__PURE__*/React.createElement("p", {
    style: {
      margin: 0,
      fontSize: `var(--font-size-${size})`,
      lineHeight: `var(--line-height-${size})`,
      color: "var(--fg-muted)",
      maxWidth: "42rem",
      ...style
    }
  }, children);
}
function SiteContainer({
  children,
  style
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      width: "100%",
      maxWidth: "90rem",
      marginInline: "auto",
      paddingInline: 32,
      ...style
    }
  }, children);
}
function ColorModeButton({
  dark,
  onToggle
}) {
  const {
    IconButton,
    Icon
  } = DS;
  return /*#__PURE__*/React.createElement(IconButton, {
    "aria-label": "Toggle color mode",
    variant: "ghost",
    size: "sm",
    onClick: onToggle
  }, /*#__PURE__*/React.createElement(Icon, {
    name: dark ? "moon" : "sun"
  }));
}
function GithubButton() {
  const {
    IconButton,
    Icon
  } = DS;
  return /*#__PURE__*/React.createElement(IconButton, {
    "aria-label": "GitHub",
    variant: "ghost",
    size: "sm"
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "github"
  }));
}
Object.assign(window, {
  DS,
  Logo,
  BlitzIcon,
  HighlightHeading,
  Subheading,
  SiteContainer,
  ColorModeButton,
  GithubButton
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/website/Shared.jsx", error: String((e && e.message) || e) }); }

__ds_ns.Button = __ds_scope.Button;

__ds_ns.IconButton = __ds_scope.IconButton;

__ds_ns.Link = __ds_scope.Link;

__ds_ns.Avatar = __ds_scope.Avatar;

__ds_ns.AvatarGroup = __ds_scope.AvatarGroup;

__ds_ns.Badge = __ds_scope.Badge;

__ds_ns.Blockquote = __ds_scope.Blockquote;

__ds_ns.Card = __ds_scope.Card;

__ds_ns.Code = __ds_scope.Code;

__ds_ns.Kbd = __ds_scope.Kbd;

__ds_ns.List = __ds_scope.List;

__ds_ns.Stat = __ds_scope.Stat;

__ds_ns.Status = __ds_scope.Status;

__ds_ns.Table = __ds_scope.Table;

__ds_ns.Tag = __ds_scope.Tag;

__ds_ns.Alert = __ds_scope.Alert;

__ds_ns.EmptyState = __ds_scope.EmptyState;

__ds_ns.Progress = __ds_scope.Progress;

__ds_ns.Skeleton = __ds_scope.Skeleton;

__ds_ns.SkeletonText = __ds_scope.SkeletonText;

__ds_ns.Spinner = __ds_scope.Spinner;

__ds_ns.Toast = __ds_scope.Toast;

__ds_ns.Checkbox = __ds_scope.Checkbox;

__ds_ns.Field = __ds_scope.Field;

__ds_ns.INPUT_SIZES = __ds_scope.INPUT_SIZES;

__ds_ns.Input = __ds_scope.Input;

__ds_ns.NativeSelect = __ds_scope.NativeSelect;

__ds_ns.PinInput = __ds_scope.PinInput;

__ds_ns.Radio = __ds_scope.Radio;

__ds_ns.RadioGroup = __ds_scope.RadioGroup;

__ds_ns.SegmentGroup = __ds_scope.SegmentGroup;

__ds_ns.Slider = __ds_scope.Slider;

__ds_ns.Switch = __ds_scope.Switch;

__ds_ns.Textarea = __ds_scope.Textarea;

__ds_ns.Icon = __ds_scope.Icon;

__ds_ns.ICON_NAMES = __ds_scope.ICON_NAMES;

__ds_ns.Container = __ds_scope.Container;

__ds_ns.Heading = __ds_scope.Heading;

__ds_ns.Separator = __ds_scope.Separator;

__ds_ns.Accordion = __ds_scope.Accordion;

__ds_ns.Breadcrumb = __ds_scope.Breadcrumb;

__ds_ns.Tabs = __ds_scope.Tabs;

__ds_ns.Dialog = __ds_scope.Dialog;

__ds_ns.Menu = __ds_scope.Menu;

__ds_ns.Popover = __ds_scope.Popover;

__ds_ns.Tooltip = __ds_scope.Tooltip;

__ds_ns.PALETTES = __ds_scope.PALETTES;

__ds_ns.TEXT = __ds_scope.TEXT;

})();
