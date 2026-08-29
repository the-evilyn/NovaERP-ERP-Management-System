import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createClients,
  deleteClients,
  getClient,
  getClients,
  updateClient,
} from "@/services/services.mock";
import type { Client } from "@/types/models";

export function useClients(page = 0, size = 10) {
  return useQuery({
    queryKey: ["clients", page, size],
    queryFn: () => getClients(page, size),
  });
}

export function useClient(id: number) {
  return useQuery({
    queryKey: ["clients", id],
    queryFn: () => getClient(id),
    enabled: Number.isFinite(id),
  });
}

export function useDeleteClients() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (ids: number[]) => deleteClients(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clients"] });
    },
  });
}

export function useCreateClients() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (
      clients: Omit<Client, "id" | "createdAt" | "updatedAt">[],
    ) => createClients(clients),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clients"] });
    },
  });
}

export function useUpdateClient() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: number;
      data: Omit<Client, "id" | "createdAt" | "updatedAt">;
    }) => updateClient(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["clients"] });
    },
  });
}
