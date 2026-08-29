import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createUnit,
  deleteUnit,
  getUnit,
  getUnits,
  updateUnit,
} from "@/services/units.service";
import type { UnitRequest } from "@/types/models";

export function useUnits(page = 0, size = 20) {
  return useQuery({
    queryKey: ["units", page, size],
    queryFn: () => getUnits(page, size),
  });
}

export function useUnit(id: number) {
  return useQuery({
    queryKey: ["units", id],
    queryFn: () => getUnit(id),
    enabled: Number.isFinite(id),
  });
}

export function useCreateUnit() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: UnitRequest) => createUnit(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["units"] });
    },
  });
}

export function useUpdateUnit() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UnitRequest }) =>
      updateUnit(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["units"] });
    },
  });
}

export function useDeleteUnit() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: number) => deleteUnit(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["units"] });
    },
  });
}
