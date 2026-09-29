import type { ReactNode } from 'react';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { PermissionProvider } from './auth/PermissionContext';
import { HomeRedirect, Layout } from './components/Layout';
import { NoticeProvider } from './components/Notice';
import { RequireMenu } from './components/RequireMenu';
import AccessConditionPage from './pages/AccessConditionPage';
import ApiKeyPage from './pages/ApiKeyPage';
import BookDetailPage from './pages/BookDetailPage';
import BookListPage from './pages/BookListPage';
import BookManagementPage from './pages/BookManagementPage';
import DashboardPage from './pages/DashboardPage';
import LoanManagementPage from './pages/LoanManagementPage';
import LoginPage from './pages/LoginPage';
import MyLoansPage from './pages/MyLoansPage';
import RoleManagementPage from './pages/RoleManagementPage';
import SignupPage from './pages/SignupPage';
import UserManagementPage from './pages/UserManagementPage';

/** 메뉴 권한이 필요한 화면 목록 (경로, 필요한 메뉴 코드 중 하나, 화면) */
const GUARDED: { path: string; menus: string[]; element: ReactNode }[] = [
  { path: '/dashboard', menus: ['DASHBOARD'], element: <DashboardPage /> },
  { path: '/books', menus: ['BOOK'], element: <BookListPage /> },
  { path: '/books/:id', menus: ['BOOK', 'BOOK_MANAGE'], element: <BookDetailPage /> },
  { path: '/my-loans', menus: ['MY_LOAN'], element: <MyLoansPage /> },
  { path: '/loan-management', menus: ['LOAN_MANAGE'], element: <LoanManagementPage /> },
  { path: '/book-management', menus: ['BOOK_MANAGE'], element: <BookManagementPage /> },
  { path: '/user-management', menus: ['USER_MANAGE'], element: <UserManagementPage /> },
  { path: '/role-management', menus: ['ROLE_MANAGE'], element: <RoleManagementPage /> },
  { path: '/access-conditions', menus: ['ACCESS_CONDITION'], element: <AccessConditionPage /> },
  { path: '/api-keys', menus: ['API_KEY'], element: <ApiKeyPage /> },
];

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route element={<Layout />}>
        <Route index element={<HomeRedirect />} />
        {GUARDED.map(({ path, menus, element }) => (
          <Route key={path} path={path} element={<RequireMenu codes={menus}>{element}</RequireMenu>} />
        ))}
        <Route path="*" element={<HomeRedirect />} />
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <NoticeProvider>
        <AuthProvider>
          <PermissionProvider>
            <AppRoutes />
          </PermissionProvider>
        </AuthProvider>
      </NoticeProvider>
    </BrowserRouter>
  );
}
