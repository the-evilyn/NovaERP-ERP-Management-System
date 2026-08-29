"use client";

import {
  AlertCircleIcon,
  Download01Icon,
  Upload01Icon,
} from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import type React from "react";
import { useRef, useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardDescription,
  CardHeader,
  CardPanel,
  CardTitle,
} from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useExportCsv, useImportCsv } from "@/hooks/use-import-export";
import { getApiErrorMessage } from "@/lib/api-error";
import type { ImportExportEntity } from "@/services/import-export.service";
import type { ImportResultResponse } from "@/types/models";

export function ImportExportCard({
  entity,
  title,
  description,
  columns,
}: {
  entity: ImportExportEntity;
  title: string;
  description: string;
  columns: string[];
}): React.ReactElement {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [result, setResult] = useState<ImportResultResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const exportMutation = useExportCsv();
  const importMutation = useImportCsv(entity);

  const handleExport = async () => {
    setError(null);
    try {
      await exportMutation.mutateAsync(entity);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'exporter les données."));
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = "";
    if (!file) return;

    setError(null);
    setResult(null);
    try {
      const importResult = await importMutation.mutateAsync(file);
      setResult(importResult);
    } catch (err) {
      setError(getApiErrorMessage(err, "Impossible d'importer le fichier."));
    }
  };

  const issues = [
    ...(result?.errors ?? []).map((issue) => ({ ...issue, level: "error" as const })),
    ...(result?.warnings ?? []).map((issue) => ({ ...issue, level: "warning" as const })),
  ].sort((a, b) => a.row - b.row);

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardPanel className="flex flex-col gap-4 pt-0">
        <p className="text-muted-foreground text-xs">
          Colonnes attendues :{" "}
          <code className="rounded bg-muted px-1 py-0.5 font-mono">
            {columns.join(", ")}
          </code>
        </p>

        {error && (
          <Alert variant="error">
            <HugeiconsIcon icon={AlertCircleIcon} />
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <div className="flex flex-wrap gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handleExport}
            loading={exportMutation.isPending}
          >
            <HugeiconsIcon icon={Download01Icon} strokeWidth={2} />
            Exporter (CSV)
          </Button>
          <Button
            variant="outline"
            size="sm"
            onClick={() => fileInputRef.current?.click()}
            loading={importMutation.isPending}
          >
            <HugeiconsIcon icon={Upload01Icon} strokeWidth={2} />
            Importer (CSV)
          </Button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,text/csv"
            className="hidden"
            onChange={handleFileChange}
          />
        </div>

        {result && (
          <div className="flex flex-col gap-3">
            <div className="flex flex-wrap gap-2">
              <Badge variant="success">{result.created} créé(s)</Badge>
              <Badge variant="secondary">
                {result.skipped} ignoré(s) (déjà existant)
              </Badge>
              {result.failed > 0 && (
                <Badge variant="destructive">{result.failed} échoué(s)</Badge>
              )}
              {result.warnings.length > 0 && (
                <Badge variant="warning">
                  {result.warnings.length} avertissement(s)
                </Badge>
              )}
            </div>

            {issues.length > 0 && (
              <ScrollArea className="h-48 rounded-md border">
                <div className="flex flex-col divide-y">
                  {issues.map((issue, idx) => (
                    <div
                      key={`${issue.row}-${idx}`}
                      className="flex items-start gap-2 px-3 py-2 text-xs"
                    >
                      <Badge
                        variant={issue.level === "error" ? "destructive" : "warning"}
                        size="sm"
                      >
                        L{issue.row}
                      </Badge>
                      <span className="text-muted-foreground">
                        {issue.identifier && (
                          <span className="font-medium text-foreground">
                            {issue.identifier} —{" "}
                          </span>
                        )}
                        {issue.message}
                      </span>
                    </div>
                  ))}
                </div>
              </ScrollArea>
            )}
          </div>
        )}
      </CardPanel>
    </Card>
  );
}
