import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth/AuthContext';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';

/** 로그인 후 첫 화면(이후 단계에서 레이아웃·메뉴로 교체) */
function Home() {
  const { me, loading, logout } = useAuth();
  if (loading) {
    return <p className="muted">불러오는 중…</p>;
  }
  if (!me) {
    return <Navigate to="/login" replace />;
  }
  return (
    <div className="card">
      <p>{me.name}님 환영합니다.</p>
      <button className="btn" onClick={logout}>
        로그아웃
      </button>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="*" element={<Home />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
