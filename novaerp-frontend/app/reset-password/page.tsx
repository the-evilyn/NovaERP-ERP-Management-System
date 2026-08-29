"use client";

import { AlertCircleIcon, CheckmarkCircle01Icon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import type React from "react";
import { Suspense, useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { resetPassword } from "@/services/auth.service";

function ResetPasswordForm(): React.ReactElement {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";

  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [done, setDone] = useState(false);

  async function handleSubmit(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await resetPassword({ token, newPassword });
      setDone(true);
    } catch {
      setError("Le lien est invalide ou a expiré.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!token) {
    return (
      <Alert variant="error">
        <HugeiconsIcon icon={AlertCircleIcon} />
        <AlertDescription>
          Lien de réinitialisation manquant ou invalide.
        </AlertDescription>
      </Alert>
    );
  }

  if (done) {
    return (
      <Alert variant="success">
        <HugeiconsIcon icon={CheckmarkCircle01Icon} />
        <AlertDescription>
          Votre mot de passe a été réinitialisé. Vous pouvez maintenant vous
          connecter.
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
      {error && (
        <Alert variant="error">
          <HugeiconsIcon icon={AlertCircleIcon} />
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <Field>
        <FieldLabel htmlFor="newPassword">Nouveau mot de passe</FieldLabel>
        <Input
          id="newPassword"
          nativeInput
          type="password"
          autoComplete="new-password"
          required
          minLength={6}
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          placeholder="6 caractères minimum"
        />
      </Field>

      <Button type="submit" className="mt-2 w-full" loading={submitting}>
        Réinitialiser le mot de passe
      </Button>
    </form>
  );
}

export default function ResetPasswordPage(): React.ReactElement {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <div className="flex aspect-square size-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <span className="font-heading font-semibold text-lg">N</span>
          </div>
          <h1 className="font-heading font-semibold text-xl">
            Nouveau mot de passe
          </h1>
        </div>

        <Suspense>
          <ResetPasswordForm />
        </Suspense>

        <p className="mt-6 text-center text-muted-foreground text-sm">
          <Link href="/login" className="text-foreground hover:underline">
            Retour à la connexion
          </Link>
        </p>
      </div>
    </div>
  );
}
