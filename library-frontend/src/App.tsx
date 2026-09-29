import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { PermissionProvider } from './auth/PermissionContext';
import { HomeRedirect, Layout } from './components/Layout';
import { NoticeProvider } from './components/Notice';
import { RequireMenu } from './components/RequireMenu';
import BookDetailPage from './pages/BookDetailPage';
import BookListPage from './pages/BookListPage';
import BookManagementPage from './pages/BookManagementPage';
import DashboardPage from './pages/DashboardPage';
import LoanManagementPage from './pages/LoanManagementPage';
import MyLoansPage from './pages/MyLoansPage';
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import UserManagementPage from './pages/UserManagementPage';

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
                <Route path="/books" element={<RequireMenu codes={['BOOK']}><BookListPage /></RequireMenu>} />
                <Route
                  path="/books/:id"
                  element={<RequireMenu codes={['BOOK', 'BOOK_MANAGE']}><BookDetailPage /></RequireMenu>}
                />
                <Route path="/my-loans" element={<RequireMenu codes={['MY_LOAN']}><MyLoansPage /></RequireMenu>} />
                <Route
                  path="/loan-management"
                  element={<RequireMenu codes={['LOAN_MANAGE']}><LoanManagementPage /></RequireMenu>}
                />
                <Route
                  path="/book-management"
                  element={<RequireMenu codes={['BOOK_MANAGE']}><BookManagementPage /></RequireMenu>}
                />
                <Route
                  path="/user-management"
                  element={<RequireMenu codes={['USER_MANAGE']}><UserManagementPage /></RequireMenu>}
                />
                <Route path="/dashboard" element={<RequireMenu codes={['DASHBOARD']}><DashboardPage /></RequireMenu>} />
                <Route path="*" element={<HomeRedirect />} />
              </Route>
            </Routes>
          </PermissionProvider>
        </AuthProvider>
      </NoticeProvider>
    </BrowserRouter>
  );
}
