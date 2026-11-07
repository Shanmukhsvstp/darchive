import Link from 'next/link';
import './NavBar.css';

export default function NavBar() {
  return (
    <div>
    <header className='header'>
      <div><Link href={'/'}>The D'Archive</Link></div>
    </header>
    </div>
  )
}
