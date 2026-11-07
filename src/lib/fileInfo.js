/** Works out how a resource can be previewed in the browser. */
export const hrefFor = (url) => url; // same-origin: "/api/files/..." or an absolute external URL

export function describeResource(r) {
  const url = r.fileUrl || "";
  const internal = url.startsWith("/api/files/");
  const ext = (url.split("?")[0].split(".").pop() || "").toLowerCase();

  const drive = url.match(/drive\.google\.com\/file\/d\/([^/?#]+)/);
  if (drive) {
    return {
      type: "drive",
      label: "DRIVE",
      embed: `https://drive.google.com/file/d/${drive[1]}/preview`,
      thumb: `https://drive.google.com/thumbnail?id=${drive[1]}&sz=w400`,
      canSummarize: false,
    };
  }
  if (["png", "jpg", "jpeg"].includes(ext) && internal) {
    return { type: "image", label: "IMG", embed: url, thumb: url, canSummarize: false };
  }
  if (ext === "pdf" && internal) {
    return { type: "pdf", label: "PDF", embed: url, thumb: null, canSummarize: true };
  }
  if (ext === "txt" && internal) {
    return { type: "text", label: "TXT", embed: url, thumb: null, canSummarize: true };
  }
  const label = { ppt: "PPT", pptx: "PPT", doc: "DOC", docx: "DOC", zip: "ZIP" }[ext] || "LINK";
  return { type: "file", label, embed: null, thumb: null, canSummarize: false };
}
