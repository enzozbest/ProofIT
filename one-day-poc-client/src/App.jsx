import { Routes, Route } from "react-router-dom";
import Chat from './pages/Chat'
import Home from './pages/Home'
import Generate1 from "./pages/Generate1";

function App() {

  return (
    <>
        <Routes>
            <Route path="/chat" element={<Chat />} />
            <Route path="/generate" element={<Generate1 />} />
            <Route path="/" element={<Home />} />
        </Routes>
    </>
  )
}

export default App
