import { api } from '@/lib/axios';

export interface UserListItem {
  id: number;
  fullName: string;
  email: string;
  role: 'ADMIN' | 'USER';
  enabled: boolean;
  createdAt: string;
}

export interface PaginatedUsersResponse {
  content: UserListItem[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const usersService = {
  getUsers: async (params?: { page?: number; size?: number; sort?: string }): Promise<PaginatedUsersResponse> => {
    const { data } = await api.get<PaginatedUsersResponse>('/users', { params });
    return data;
  },

  updateUserRole: async (id: number, role: 'ADMIN' | 'USER'): Promise<UserListItem> => {
    const { data } = await api.patch<UserListItem>(`/users/${id}/role`, { role });
    return data;
  },

  updateUserStatus: async (id: number, enabled: boolean): Promise<UserListItem> => {
    const { data } = await api.patch<UserListItem>(`/users/${id}/status`, { enabled });
    return data;
  },
};
