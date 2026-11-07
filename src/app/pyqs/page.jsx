import ResourceBrowser from "@/components/ResourceBrowser/ResourceBrowser";

function ExternalPyqLinks() {
  return (
    <section className="ext-links">
      <strong>Looking for more question papers? Other BMSCE student-run sites:</strong>
      <ul>
        <li>
          <a href="https://notego.in/" target="_blank" rel="noopener noreferrer">
            NoteGo - notes, papers &amp; placements
          </a>
        </li>
        <li>
          <a href="https://exnote.vercel.app/" target="_blank" rel="noopener noreferrer">
            ExNote - notes for BMSCE
          </a>
        </li>
      </ul>
    </section>
  );
}

export default function PyqsPage() {
  return <ResourceBrowser kind="pyq" heading="Previous Year Questions" extra={<ExternalPyqLinks />} />;
}
