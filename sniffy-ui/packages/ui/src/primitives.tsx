import { Collapsible as BaseCollapsible } from '@base-ui/react/collapsible';
import { NumberField as BaseNumberField } from '@base-ui/react/number-field';
import { Switch as BaseSwitch } from '@base-ui/react/switch';
import { Tabs as BaseTabs } from '@base-ui/react/tabs';
import { Tooltip as BaseTooltip } from '@base-ui/react/tooltip';
import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from 'react';
import { ChevronDown, Minus, Plus } from 'lucide-react';

export function cx(...values: Array<string | false | null | undefined>): string {
  return values.filter(Boolean).join(' ');
}

export function Button({ className, ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return (
    <button
      className={cx(
        'inline-flex min-h-9 items-center justify-center gap-2 rounded-md border border-border bg-surface-raised px-3 py-2 text-sm font-medium text-foreground transition hover:bg-surface-hover focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-focus disabled:cursor-not-allowed disabled:opacity-50',
        className,
      )}
      {...props}
    />
  );
}

export function IconButton({ className, ...props }: ButtonHTMLAttributes<HTMLButtonElement>) {
  return <Button className={cx('size-9 p-0', className)} {...props} />;
}

export function Badge({ className, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cx('inline-flex rounded-full border border-border px-2 py-0.5 text-xs', className)}
      {...props}
    />
  );
}

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cx('rounded-lg border border-border bg-surface shadow-sm', className)}
      {...props}
    />
  );
}

export function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className={cx(
        'min-h-9 rounded-md border border-border bg-canvas px-2 text-foreground focus-visible:outline-2 focus-visible:outline-focus',
        props.className,
      )}
    />
  );
}

export function Switch({
  checked,
  onCheckedChange,
  label,
  disabled = false,
}: {
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  label: string;
  disabled?: boolean;
}) {
  return (
    <BaseSwitch.Root
      checked={checked}
      onCheckedChange={onCheckedChange}
      aria-label={label}
      disabled={disabled}
      className="relative h-6 w-11 rounded-full border border-border bg-canvas transition data-[checked]:bg-accent focus-visible:outline-2 focus-visible:outline-focus"
    >
      <BaseSwitch.Thumb
        className="block size-5 rounded-full bg-foreground transition-transform"
        style={{ transform: checked ? 'translateX(20px)' : 'translateX(2px)' }}
      />
    </BaseSwitch.Root>
  );
}

export function NumberField({
  value,
  onValueChange,
  label,
  disabled = false,
}: {
  value: number;
  onValueChange: (value: number) => void;
  label: string;
  disabled?: boolean;
}) {
  return (
    <BaseNumberField.Root
      value={value}
      onValueChange={(next) => onValueChange(next ?? 0)}
      min={0}
      aria-label={label}
      disabled={disabled}
    >
      <BaseNumberField.Group className="inline-flex overflow-hidden rounded-md border border-border bg-canvas">
        <BaseNumberField.Decrement
          className="grid size-8 place-items-center hover:bg-surface-hover"
          aria-label={`Decrease ${label}`}
        >
          <Minus size={14} />
        </BaseNumberField.Decrement>
        <BaseNumberField.Input
          aria-label={label}
          className="w-14 bg-transparent text-center text-sm outline-none"
        />
        <BaseNumberField.Increment
          className="grid size-8 place-items-center hover:bg-surface-hover"
          aria-label={`Increase ${label}`}
        >
          <Plus size={14} />
        </BaseNumberField.Increment>
      </BaseNumberField.Group>
    </BaseNumberField.Root>
  );
}

export const Tabs = BaseTabs;

export function Collapsible({
  title,
  children,
  defaultOpen = false,
}: {
  title: string;
  children: ReactNode;
  defaultOpen?: boolean;
}) {
  return (
    <BaseCollapsible.Root defaultOpen={defaultOpen} className="rounded-md border border-border">
      <BaseCollapsible.Trigger className="flex w-full items-center justify-between p-2 text-left text-sm hover:bg-surface-hover">
        {title}
        <ChevronDown className="transition-transform data-[panel-open]:rotate-180" size={16} />
      </BaseCollapsible.Trigger>
      <BaseCollapsible.Panel className="overflow-hidden border-t border-border p-2 text-sm data-[ending-style]:h-0 data-[starting-style]:h-0">
        {children}
      </BaseCollapsible.Panel>
    </BaseCollapsible.Root>
  );
}

export function Tooltip({
  label,
  children,
  portalRoot,
}: {
  label: string;
  children: ReactNode;
  portalRoot?: HTMLElement | ShadowRoot;
}) {
  return (
    <BaseTooltip.Provider>
      <BaseTooltip.Root>
        <BaseTooltip.Trigger render={children as React.ReactElement} />
        <BaseTooltip.Portal container={portalRoot}>
          <BaseTooltip.Positioner sideOffset={6}>
            <BaseTooltip.Popup className="z-50 rounded-md border border-border bg-surface-raised px-2 py-1 text-xs text-foreground shadow-lg">
              {label}
              <BaseTooltip.Arrow className="fill-surface-raised" />
            </BaseTooltip.Popup>
          </BaseTooltip.Positioner>
        </BaseTooltip.Portal>
      </BaseTooltip.Root>
    </BaseTooltip.Provider>
  );
}

export function Table({ children, label }: { children: ReactNode; label: string }) {
  return (
    <table
      aria-label={label}
      className="w-full border-collapse text-left text-sm [&_td]:border-t [&_td]:border-border [&_td]:p-2 [&_th]:p-2 [&_th]:text-muted"
    >
      {children}
    </table>
  );
}

export function StateMessage({
  kind,
  children,
}: {
  kind: 'loading' | 'empty' | 'error';
  children: ReactNode;
}) {
  return (
    <div
      role={kind === 'error' ? 'alert' : 'status'}
      className={cx(
        'm-4 rounded-md border border-border p-4 text-center text-muted',
        kind === 'error' && 'border-danger text-danger',
      )}
    >
      {children}
    </div>
  );
}

export function CodeViewer({ code, language = 'text' }: { code: string; language?: string }) {
  return (
    <pre className="max-w-full overflow-auto whitespace-pre-wrap rounded-md bg-canvas p-3 font-mono text-xs text-foreground">
      <code data-language={language}>{code}</code>
    </pre>
  );
}
