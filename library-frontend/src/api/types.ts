// 백엔드 API 응답·요청 타입

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ErrorResponse {
  code: string;
  message: string;
  path: string;
  timestamp: string;
  fieldErrors: FieldError[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Me {
  id: number;
  username: string;
  name: string;
  email: string;
  roleCode: string;
  roleName: string;
}

/** 호출 가능한 "METHOD /pattern" 목록과 진입 가능한 메뉴 코드 */
export interface MyPermissions {
  urls: string[];
  menus: string[];
}

export interface Book {
  id: number;
  isbn: string;
  title: string;
  author: string;
  publisher: string;
  category: string;
  totalQuantity: number;
  availableQuantity: number;
}

export type BookRequest = Omit<Book, 'id' | 'availableQuantity'>;

export type LoanStatus = 'LOANED' | 'RETURNED' | 'OVERDUE';

export interface Loan {
  id: number;
  bookId: number;
  bookTitle: string;
  userId: number;
  username: string;
  loanDate: string;
  dueDate: string;
  returnedDate: string | null;
  status: LoanStatus;
}

export interface DashboardStats {
  titles: number;
  totalCopies: number;
  availableCopies: number;
  activeLoans: number;
  overdueLoans: number;
  users: number;
}

export interface Role {
  id: number;
  code: string;
  name: string;
  system: boolean;
}

export interface AdminUser {
  id: number;
  username: string;
  name: string;
  email: string;
  roleId: number;
  roleCode: string;
  roleName: string;
  locked: boolean;
  failedLoginCount: number;
  createdAt: string;
}

export interface UrlNode {
  id: number;
  httpMethod: string;
  urlPattern: string;
}

export interface ActionNode {
  id: number;
  code: string;
  name: string;
  actionType: 'READ' | 'ACTION';
  authorityCode: string;
  urls: UrlNode[];
}

export interface MenuTree {
  id: number;
  code: string;
  name: string;
  path: string;
  sortOrder: number;
  actions: ActionNode[];
}

export interface AccessCondition {
  userId: number;
  username: string;
  name: string;
  roleCode: string;
  configured: boolean;
  allowedIps: string[];
  validFrom: string | null;
  validTo: string | null;
  allowedDays: string[];
  startTime: string | null;
  endTime: string | null;
}

export interface AccessConditionRequest {
  allowedIps: string[];
  validFrom: string | null;
  validTo: string | null;
  allowedDays: string[];
  startTime: string | null;
  endTime: string | null;
}

export interface ApiKey {
  id: number;
  name: string;
  keyPrefix: string;
  ownerUserId: number;
  actionCodes: string[];
  allowedIps: string[];
  expiresAt: string | null;
  revokedAt: string | null;
  createdAt: string;
  lastUsedAt: string | null;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
}

export interface IssuedApiKey {
  apiKey: string;
  detail: ApiKey;
}
