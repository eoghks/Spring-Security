import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { PermissionProvider } from './auth/PermissionContext';
import { HomeRedirect, Layout } from './components/Layout';
import { NoticeProvider } from './components/Notice';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';

export default function App() {
  return (
    <BrowserRouter>
      <NoticeProvider>
        <AuthProvider>
          <PermissionProvider>
            <Routes>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/signup" element={<SignupPage />} />
              <Route element={<Layout />}>
                <Route index element={<HomeRedirect />} />
                <Route path="*" element={<HomeRedirect />} />
              </Route>
            </Routes>
          </PermissionProvider>
        </AuthProvider>
      </NoticeProvider>
    </BrowserRouter>
  );
}
