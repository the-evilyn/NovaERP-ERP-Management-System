import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  exportCsv,
  importCsv,
  type ImportExportEntity,
} from "@/services/import-export.service";

export function useExportCsv() {
  return useMutation({
    mutationFn: (entity: ImportExportEntity) => exportCsv(entity),
  });
}

export function useImportCsv(entity: ImportExportEntity) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (file: File) => importCsv(entity, file),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: [entity] });
    },
  });
}
