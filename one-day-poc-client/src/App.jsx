import { Routes, Route } from "react-router-dom";
import Chat from './pages/Chat'
import Home from './pages/Home'

function App() {

  return (
    <>
        <Routes>
            <Route path="/chat" element={<Chat />} />
            <Route path="/" element={<Home />} />
        </Routes>
    </>
  )
}

export default App
