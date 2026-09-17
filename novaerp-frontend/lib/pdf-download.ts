// lib/pdf-download.ts
// Utilities for downloading and previewing server-generated PDF blobs

export function openPdfBlob(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  const win = window.open(url, '_blank');
  if (!win) {
    // Popup blocker fallback: trigger direct download
    downloadPdfBlob(blob, 'document.pdf');
    return;
  }
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}

export function downloadPdfBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  setTimeout(() => URL.revokeObjectURL(url), 60000);
}

export function extractPdfFilename(dispositionHeader?: string, fallback = 'document.pdf'): string {
  if (!dispositionHeader) return fallback;
  const match = dispositionHeader.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/i);
  if (match && match[1]) {
    return match[1].replace(/['"]/g, '').trim();
  }
  return fallback;
}
