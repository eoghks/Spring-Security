import { api } from './client';
import type {
  AccessCondition,
  AccessConditionRequest,
  AdminUser,
  ApiKey,
  Book,
  BookRequest,
  DashboardStats,
  IssuedApiKey,
  Loan,
  Me,
  MenuTree,
  MyPermissions,
  Page,
  Role,
  TokenResponse,
  UrlNode,
} from './types';

// 백엔드 엔드포인트 호출 함수 모음

export const authApi = {
  login: (username: string, password: string) =>
    api.post<TokenResponse>('/api/auth/login', { username, password }).then((r) => r.data),
  signup: (body: { username: string; password: string; name: string; email: string }) =>
    api.post('/api/auth/signup', body).then((r) => r.data),
  logout: (refreshToken: string) => api.post('/api/auth/logout', { refreshToken }),
};

export const meApi = {
  me: () => api.get<Me>('/api/me').then((r) => r.data),
  permissions: () => api.get<MyPermissions>('/api/me/permissions').then((r) => r.data),
};

export const bookApi = {
  search: (params: { keyword?: string; category?: string; page?: number; size?: number }) =>
    api.get<Page<Book>>('/api/books', { params }).then((r) => r.data),
  get: (id: number) => api.get<Book>(`/api/books/${id}`).then((r) => r.data),
  categories: () => api.get<string[]>('/api/books/categories').then((r) => r.data),
  create: (body: BookRequest) => api.post<Book>('/api/books', body).then((r) => r.data),
  update: (id: number, body: BookRequest) => api.put<Book>(`/api/books/${id}`, body).then((r) => r.data),
  remove: (id: number) => api.delete(`/api/books/${id}`),
};

export const loanApi = {
  borrow: (bookId: number) => api.post<Loan>('/api/loans', { bookId }).then((r) => r.data),
  myLoans: () => api.get<Loan[]>('/api/loans/me').then((r) => r.data),
  returnMine: (id: number) => api.post<Loan>(`/api/loans/${id}/return`).then((r) => r.data),
};

export const loanManagementApi = {
  search: (params: { status?: string; keyword?: string; page?: number; size?: number }) =>
    api.get<Page<Loan>>('/api/loan-management', { params }).then((r) => r.data),
  checkout: (username: string, bookId: number) =>
    api.post<Loan>('/api/loan-management', { username, bookId }).then((r) => r.data),
  processReturn: (id: number) => api.post<Loan>(`/api/loan-management/${id}/return`).then((r) => r.data),
};

export const dashboardApi = {
  stats: () => api.get<DashboardStats>('/api/dashboard/stats').then((r) => r.data),
};

export const userAdminApi = {
  search: (params: { keyword?: string; page?: number; size?: number }) =>
    api.get<Page<AdminUser>>('/api/admin/users', { params }).then((r) => r.data),
  changeRole: (id: number, roleId: number) =>
    api.put<AdminUser>(`/api/admin/users/${id}/role`, { roleId }).then((r) => r.data),
  unlock: (id: number) => api.post<AdminUser>(`/api/admin/users/${id}/unlock`).then((r) => r.data),
};

export const authzAdminApi = {
  roles: () => api.get<Role[]>('/api/admin/roles').then((r) => r.data),
  menus: () => api.get<MenuTree[]>('/api/admin/menus').then((r) => r.data),
  roleActions: (roleId: number) =>
    api.get<{ roleId: number; actionIds: number[] }>(`/api/admin/roles/${roleId}/actions`).then((r) => r.data),
  saveRoleActions: (roleId: number, actionIds: number[]) =>
    api.put(`/api/admin/roles/${roleId}/actions`, { actionIds }).then((r) => r.data),
  addActionUrl: (actionId: number, httpMethod: string, urlPattern: string) =>
    api.post<UrlNode>(`/api/admin/actions/${actionId}/urls`, { httpMethod, urlPattern }).then((r) => r.data),
  deleteActionUrl: (id: number) => api.delete(`/api/admin/action-urls/${id}`),
  reload: () => api.post('/api/admin/authz/reload'),
};

export const accessConditionApi = {
  list: (params: { keyword?: string; page?: number; size?: number }) =>
    api.get<Page<AccessCondition>>('/api/admin/access-conditions', { params }).then((r) => r.data),
  save: (userId: number, body: AccessConditionRequest) =>
    api.put<AccessCondition>(`/api/admin/access-conditions/${userId}`, body).then((r) => r.data),
  remove: (userId: number) => api.delete(`/api/admin/access-conditions/${userId}`),
};

export const apiKeyApi = {
  list: () => api.get<ApiKey[]>('/api/admin/api-keys').then((r) => r.data),
  issue: (body: { name: string; actionIds: number[]; allowedIps: string[]; expiresAt: string | null }) =>
    api.post<IssuedApiKey>('/api/admin/api-keys', body).then((r) => r.data),
  revoke: (id: number) => api.post<ApiKey>(`/api/admin/api-keys/${id}/revoke`).then((r) => r.data),
};
