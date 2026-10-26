import './NavBar.css';

export default function NavBar() {
  return (
    <div>
    <header>
      <h1 id="Login_h">Student Login</h1>
    </header>
    <br/>
    <main>
      <form action="https://httpbin.org/post" method="post">
        <p>
          <label htmlFor="email">Email : </label>
          <input type="email" name="email" id="email" placeholder="urname@bmsce.ac.in" />
        </p>
        
        <br />

        <p>
          <label htmlFor="passw">Password: </label>
          <input type="password" name="passw" id="passw" />
        </p>

        <br />

        <button type="submit" className="subm">Login</button>
      </form>
    </main>
    </div>
  )
}
