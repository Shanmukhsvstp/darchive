"use client";

import "./home.css";
import Link from "next/link";
import { useRouter } from "next/navigation";

export default function Home() {

  const router = useRouter();

  async function handleLogout() {
    try {
      const response = await fetch(
        "http://localhost:2026/api/auth/logout",
        {
          method: "POST",
          credentials: "include",
        }
      );

      if (response.ok) {
        router.replace("/auth");
        router.refresh();
      } else {
        console.error("Logout failed");
      }
    } catch (error) {
      console.error("Error logging out:", error);
    }
  }

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
                    <Link href="/notes">PDFs and Notes</Link>
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
              <li>
                <article>
                  <section>
                    
                  </section>
                  <section>
                    <button onClick={handleLogout}>Logout</button>
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
