import "./page.css";

export default function NotesPage() {
  return (
    <div className="notes-root">
      <div className="flex-container">
        <main>
          {/* Subject 1 */}
          <article id="subj1" className="subs">
            <h2>Subject 1</h2>
            <br />
            <section id="Notesere" className="flex-container">
              <article className="RCard">
                <h3>Unit 1</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
              <article className="RCard">
                <h3>Unit 2</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
              <article className="RCard">
                <h3>Unit 3</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
            </section>
          </article>

          {/* Subject 2 */}
          <article id="subj2" className="subs">
            <h2>Subject 2</h2>
            <br />
            <section id="Notesere" className="flex-container">
              <article className="RCard">
                <h3>Unit 1</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
              <article className="RCard">
                <h3>Unit 2</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
              <article className="RCard">
                <h3>Unit 3</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
            </section>
          </article>

          {/* Subject 3 */}
          <article id="subj3" className="subs">
            <h2>Subject 3</h2>
            <br />
            <section id="Notesere" className="flex-container">
              <article className="RCard">
                <h3>Unit 1</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
              <article className="RCard">
                <h3>Unit 2</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
              <article className="RCard">
                <h3>Unit 3</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
            </section>
          </article>

          {/* Archive */}
          <article id="arch" className="subs">
            <h2>Archive</h2>
            <br />
            <section id="Notesere" className="flex-container">
              <article className="RCard">
                <h3>Unit 1</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
              <article className="RCard">
                <h3>Unit 2</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
              <article className="RCard">
                <h3>Unit 3</h3>
                <section>
                  <img src="https://placehold.co/300x200.png" alt="preview" />
                </section>
                <section>
                  <p>Some information about the resource</p>
                </section>
              </article>
            </section>
          </article>
        </main>

        <aside>
          <nav>
            <ul>
              <li><a href="#subj1">Subject 1</a></li>
              <li><a href="#subj2">Subject 2</a></li>
              <li><a href="#subj3">Subject 3</a></li>
              <li><a href="#arch">Archive</a></li>
            </ul>
          </nav>
        </aside>
      </div>
    </div>
  );
}