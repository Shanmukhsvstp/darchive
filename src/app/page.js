import Image from "next/image";
import "./home.css";

export default function Home() {
  return (
    <div>
      <div className="flex-container">
      <main>
        <div>
          <h2 id="Com_for">Community Forum</h2>

          <br/>

          <form>
            <p>
              <label htmlFor="post">This better be an important thing</label>

              <br/>

              <textarea name="post" id="post" cols="30" rows="10" placeholder="Write it here"></textarea>

              <br/>  

              <button type="submit" className="p_b">Post</button>
            </p>
          </form>
        </div>
        <article>
          <h3>Lorem ipsum dolor sit amet consectetur adipisicing elit.</h3>
          <section>
            <p>
              Lorem ipsum dolor sit amet consectetur, adipisicing elit. Illum dolor, beatae iste adipisci earum quisquam alias nemo similique dolorem itaque dolorum voluptates rem? Explicabo quidem labore eos? Mollitia, quidem velit?
            </p>
          </section>
        </article>
      </main>
      <aside>
          {/* <!-- put links leading to materials, pyqs and the lab thing over here --> */}
           <h2 id="Res">Resources</h2>
           <nav>
            <ul>
              <li>
                <article>
                  <section>
                    
                  </section>
                  <section>
                    <a href="notes.html">PDFs and Notes</a>
                  </section>
                </article>
              </li>
              <li>
                <article>
                  <section>
                    
                  </section>
                  <section>
                    <a href="">PYQs</a>
                  </section>
                </article>
              </li>
              <li>
                <article>
                  <section>
                    
                  </section>
                  <section>
                    <a href="">Lab Work</a>
                  </section>
                </article>
              </li>
            </ul>
           </nav>
      </aside>
    </div>
    </div>
);
}
