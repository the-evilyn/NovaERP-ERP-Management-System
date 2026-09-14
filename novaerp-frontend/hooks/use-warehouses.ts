import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  createLocation,
  createWarehouse,
  getArticleWarehouseStocks,
  getLocation,
  getLocations,
  getWarehouse,
  getWarehouses,
  getWarehouseStocks,
  setLocationActive,
  setWarehouseActive,
  updateLocation,
  updateWarehouse,
} from "@/services/warehouses.service";
import type {
  WarehouseLocationRequest,
  WarehouseRequest,
} from "@/types/models";

export function useWarehouses(page = 0, size = 20, active?: boolean) {
  return useQuery({
    queryKey: ["warehouses", page, size, active],
    queryFn: () => getWarehouses(page, size, active),
  });
}

export function useWarehouse(id: number) {
  return useQuery({
    queryKey: ["warehouses", id],
    queryFn: () => getWarehouse(id),
    enabled: Number.isFinite(id) && id > 0,
  });
}

export function useCreateWarehouse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: WarehouseRequest) => createWarehouse(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["warehouses"] });
    },
  });
}

export function useUpdateWarehouse() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: WarehouseRequest }) =>
      updateWarehouse(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["warehouses"] });
    },
  });
}

export function useToggleWarehouseActive() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) =>
      setWarehouseActive(id, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["warehouses"] });
    },
  });
}

export function useWarehouseLocations(
  warehouseId: number,
  page = 0,
  size = 20,
  active?: boolean,
) {
  return useQuery({
    queryKey: ["warehouses", warehouseId, "locations", page, size, active],
    queryFn: () => getLocations(warehouseId, page, size, active),
    enabled: Number.isFinite(warehouseId) && warehouseId > 0,
  });
}

export function useWarehouseLocation(
  warehouseId: number,
  locationId: number,
) {
  return useQuery({
    queryKey: ["warehouses", warehouseId, "locations", locationId],
    queryFn: () => getLocation(warehouseId, locationId),
    enabled:
      Number.isFinite(warehouseId) &&
      warehouseId > 0 &&
      Number.isFinite(locationId) &&
      locationId > 0,
  });
}

export function useCreateWarehouseLocation(warehouseId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: WarehouseLocationRequest) =>
      createLocation(warehouseId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["warehouses", warehouseId, "locations"],
      });
    },
  });
}

export function useUpdateWarehouseLocation(warehouseId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      locationId,
      data,
    }: {
      locationId: number;
      data: WarehouseLocationRequest;
    }) => updateLocation(warehouseId, locationId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["warehouses", warehouseId, "locations"],
      });
    },
  });
}

export function useToggleLocationActive(warehouseId: number) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      locationId,
      active,
    }: {
      locationId: number;
      active: boolean;
    }) => setLocationActive(warehouseId, locationId, active),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ["warehouses", warehouseId, "locations"],
      });
    },
  });
}

export function useWarehouseStocks(
  warehouseId: number | null,
  page = 0,
  size = 20,
  locationId?: number | null,
  positiveOnly?: boolean,
) {
  return useQuery({
    queryKey: [
      "warehouses",
      warehouseId,
      "stocks",
      page,
      size,
      locationId ?? null,
      positiveOnly ?? false,
    ],
    queryFn: () =>
      getWarehouseStocks(warehouseId as number, {
        page,
        size,
        locationId: locationId ?? undefined,
        positiveOnly,
      }),
    enabled:
      typeof warehouseId === "number" &&
      Number.isFinite(warehouseId) &&
      warehouseId > 0,
  });
}

export function useArticleWarehouseStocks(articleId: number | null) {
  return useQuery({
    queryKey: ["warehouses", "stocks", "article", articleId],
    queryFn: () => getArticleWarehouseStocks(articleId as number),
    enabled:
      typeof articleId === "number" &&
      Number.isFinite(articleId) &&
      articleId > 0,
  });
}
