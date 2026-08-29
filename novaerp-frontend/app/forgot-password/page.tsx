"use client";

import { AlertCircleIcon, CheckmarkCircle01Icon } from "@hugeicons/core-free-icons";
import { HugeiconsIcon } from "@hugeicons/react";
import Link from "next/link";
import type React from "react";
import { useState } from "react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Field, FieldLabel } from "@/components/ui/field";
import { Input } from "@/components/ui/input";
import { forgotPassword } from "@/services/auth.service";

export default function ForgotPasswordPage(): React.ReactElement {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [sent, setSent] = useState(false);

  async function handleSubmit(e: React.FormEvent): Promise<void> {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await forgotPassword({ email });
      setSent(true);
    } catch {
      setError("Une erreur est survenue. Réessayez plus tard.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center justify-center p-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center gap-2 text-center">
          <div className="flex aspect-square size-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <span className="font-heading font-semibold text-lg">N</span>
          </div>
          <h1 className="font-heading font-semibold text-xl">
            Mot de passe oublié
          </h1>
          <p className="text-muted-foreground text-sm">
            Entrez votre email pour recevoir un lien de réinitialisation
          </p>
        </div>

        {sent ? (
          <Alert variant="success">
            <HugeiconsIcon icon={CheckmarkCircle01Icon} />
            <AlertDescription>
              Si un compte existe pour cet email, un lien de réinitialisation
              vient d'être envoyé.
            </AlertDescription>
          </Alert>
        ) : (
          <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
            {error && (
              <Alert variant="error">
                <HugeiconsIcon icon={AlertCircleIcon} />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <Field>
              <FieldLabel htmlFor="email">Email</FieldLabel>
              <Input
                id="email"
                nativeInput
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="vous@novaerp.local"
              />
            </Field>

            <Button type="submit" className="mt-2 w-full" loading={submitting}>
              Envoyer le lien
            </Button>
          </form>
        )}

        <p className="mt-6 text-center text-muted-foreground text-sm">
          <Link href="/login" className="text-foreground hover:underline">
            Retour à la connexion
          </Link>
        </p>
      </div>
    </div>
  );
}
