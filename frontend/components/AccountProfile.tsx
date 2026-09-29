"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Button, LinkButton } from "@/components/Button";
import { useAuth } from "@/components/AuthProvider";
import { AuthClientError, changeCurrentUserPassword, updateCurrentUserProfile } from "@/lib/authClient";
import type { CurrentUser } from "@/lib/types";

export function AccountProfile() {
  const router = useRouter();
  const { user, accessToken, loading, refreshAuth, updateUser, clearLocalSession } = useAuth();
  const [editing, setEditing] = useState(false);
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  useEffect(() => {
    if (!loading && !user) {
      router.replace("/login");
    }
  }, [loading, router, user]);

  if (loading) {
    return (
      <section className="account-panel" aria-busy="true">
        <p className="eyebrow">Account</p>
        <div className="skeleton-line skeleton-wide" />
        <div className="skeleton-line" />
      </section>
    );
  }

  if (!user) {
    return (
      <section className="account-panel">
        <p className="eyebrow">Account</p>
        <h1>Sign in to manage your profile.</h1>
        <LinkButton href="/login">Login</LinkButton>
      </section>
    );
  }

  const currentUser = user;
  const fullName = `${currentUser.firstName} ${currentUser.lastName}`;

  async function withFreshToken<T>(action: (token: string) => Promise<T>) {
    let token = accessToken;
    if (!token) {
      const restored = await refreshAuth();
      token = restored?.accessToken ?? null;
    }

    if (!token) {
      throw new AuthClientError("Session expired. Please sign in again.", 401);
    }

    try {
      return await action(token);
    } catch (caught) {
      if (!(caught instanceof AuthClientError) || caught.status !== 401) {
        throw caught;
      }
      const restored = await refreshAuth();
      if (!restored) {
        throw caught;
      }
      return action(restored.accessToken);
    }
  }

  async function handleProfileSave(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSuccess(null);

    const nextFirstName = firstName.trim();
    const nextLastName = lastName.trim();
    if (!nextFirstName || !nextLastName) {
      setError("First name and last name are required.");
      return;
    }

    setSaving(true);
    try {
      const updated = await withFreshToken((token) => updateCurrentUserProfile(token, {
        firstName: nextFirstName,
        lastName: nextLastName
      }));
      updateUser(updated);
      setEditing(false);
      setSuccess("Profile updated.");
    } catch (caught) {
      if (caught instanceof AuthClientError && caught.status === 401) {
        router.replace("/login");
        return;
      }
      setError(caught instanceof Error ? caught.message : "Unable to update profile.");
    } finally {
      setSaving(false);
    }
  }

  async function handlePasswordChange(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPasswordError(null);

    if (newPassword !== confirmPassword) {
      setPasswordError("New password and confirmation do not match.");
      return;
    }
    if (newPassword.length < 8) {
      setPasswordError("Password must be at least 8 characters.");
      return;
    }

    setChangingPassword(true);
    try {
      await withFreshToken((token) => changeCurrentUserPassword(token, {
        currentPassword,
        newPassword,
        confirmPassword
      }));
      clearPasswordForm();
      clearLocalSession();
      router.push("/login?notice=password-changed");
      router.refresh();
    } catch (caught) {
      if (caught instanceof AuthClientError && caught.status === 401) {
        setPasswordError(caught.message);
      } else {
        setPasswordError(caught instanceof Error ? caught.message : "Unable to change password.");
      }
    } finally {
      setChangingPassword(false);
    }
  }

  function startEditing() {
    setFirstName(currentUser.firstName);
    setLastName(currentUser.lastName);
    setEditing(true);
    setError(null);
    setSuccess(null);
  }

  function handleCancel() {
    setFirstName(currentUser.firstName);
    setLastName(currentUser.lastName);
    setEditing(false);
    setError(null);
    setSuccess(null);
  }

  function clearPasswordForm() {
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setPasswordError(null);
  }

  return (
    <section className="account-layout">
      <div className="account-copy">
        <p className="eyebrow">Account</p>
        <h1>{fullName}</h1>
        <p>Your LUXORA profile is kept deliberately focused for now: name, email, account state, and password security.</p>
      </div>

      <div className="account-stack">
        <div className="account-panel">
          <div className="account-panel-header">
            <div>
              <p className="eyebrow">Profile</p>
              <h2>Personal details</h2>
            </div>
            {!editing ? (
              <Button type="button" variant="secondary" onClick={startEditing}>
                Edit
              </Button>
            ) : null}
          </div>

          {editing ? (
            <form className="account-form" onSubmit={handleProfileSave}>
              <label>
                <span>First name</span>
                <input
                  value={firstName}
                  onChange={(event) => setFirstName(event.target.value)}
                  maxLength={120}
                  autoComplete="given-name"
                  required
                />
              </label>
              <label>
                <span>Last name</span>
                <input
                  value={lastName}
                  onChange={(event) => setLastName(event.target.value)}
                  maxLength={120}
                  autoComplete="family-name"
                  required
                />
              </label>
              {error ? <p className="form-error" role="alert">{error}</p> : null}
              <div className="account-actions">
                <Button type="submit" disabled={saving}>
                  {saving ? "Saving" : "Save"}
                </Button>
                <Button type="button" variant="ghost" onClick={handleCancel} disabled={saving}>
                  Cancel
                </Button>
              </div>
            </form>
          ) : (
            <ProfileDetails user={currentUser} />
          )}

          {success ? <p className="form-success" role="status">{success}</p> : null}
        </div>

        <div className="account-panel">
          <div className="account-panel-header">
            <div>
              <p className="eyebrow">Security</p>
              <h2>Change password</h2>
            </div>
          </div>
          <form className="account-form" onSubmit={handlePasswordChange}>
            <label>
              <span>Current password</span>
              <input
                type="password"
                autoComplete="current-password"
                required
                maxLength={100}
                value={currentPassword}
                onChange={(event) => setCurrentPassword(event.target.value)}
              />
            </label>
            <label>
              <span>New password</span>
              <input
                type="password"
                autoComplete="new-password"
                required
                minLength={8}
                maxLength={100}
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
              />
            </label>
            <label>
              <span>Confirm new password</span>
              <input
                type="password"
                autoComplete="new-password"
                required
                minLength={8}
                maxLength={100}
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
              />
            </label>
            {passwordError ? <p className="form-error" role="alert">{passwordError}</p> : null}
            <div className="account-actions">
              <Button type="submit" disabled={changingPassword}>
                {changingPassword ? "Changing" : "Change password"}
              </Button>
              <Button type="button" variant="ghost" onClick={clearPasswordForm} disabled={changingPassword}>
                Cancel
              </Button>
            </div>
          </form>
        </div>
      </div>
    </section>
  );
}

function ProfileDetails({ user }: { user: CurrentUser }) {
  return (
    <dl className="profile-details">
      <div>
        <dt>Name</dt>
        <dd>{user.firstName} {user.lastName}</dd>
      </div>
      <div>
        <dt>Email</dt>
        <dd>{user.email}</dd>
      </div>
      <div>
        <dt>Status</dt>
        <dd>{user.enabled ? "Active" : "Disabled"}</dd>
      </div>
    </dl>
  );
}
