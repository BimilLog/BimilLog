"use client"

import * as React from "react"
import { Modal, ModalHeader, ModalBody, ModalFooter } from "flowbite-react"
import { cn } from "@/lib/utils"
import { X } from "lucide-react"

interface DialogProps {
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  children?: React.ReactNode;
}

const Dialog = React.memo(({ open, onOpenChange, children }: DialogProps) => {
  const childrenArray = React.Children.toArray(children);
  const content = childrenArray.find(
    (child) => React.isValidElement(child) && child.type === DialogContent
  );
  const trigger = childrenArray.find(
    (child) => React.isValidElement(child) && child.type === DialogTrigger
  );

  const [internalOpen, setInternalOpen] = React.useState(false);
  const isControlled = open !== undefined;
  const isOpen = isControlled ? open : internalOpen;

  const handleOpenChange = (newOpen: boolean) => {
    if (!isControlled) {
      setInternalOpen(newOpen);
    }
    onOpenChange?.(newOpen);
  };

  if (trigger && React.isValidElement(trigger)) {
    return (
      <>
        {React.cloneElement(trigger as React.ReactElement<any>, {
          onClick: () => handleOpenChange(true),
        })}
        {content && React.isValidElement(content) &&
          React.cloneElement(content as React.ReactElement<any>, {
            open: isOpen,
            onClose: () => handleOpenChange(false),
          })
        }
      </>
    );
  }

  return content && React.isValidElement(content) ?
    React.cloneElement(content as React.ReactElement<any>, {
      open: isOpen,
      onClose: () => handleOpenChange(false),
    }) : null;
});

Dialog.displayName = "Dialog";

const DialogTrigger = React.memo(({
  children,
  asChild,
  ...props
}: {
  children: React.ReactNode;
  asChild?: boolean;
  onClick?: () => void;
}) => {
  if (asChild && React.isValidElement(children)) {
    return React.cloneElement(children as React.ReactElement<any>, {
      ...props,
      onClick: (e: React.MouseEvent) => {
        props.onClick?.();
        (children as any).props?.onClick?.(e);
      },
    });
  }

  return (
    <button {...props}>
      {children}
    </button>
  );
});

DialogTrigger.displayName = "DialogTrigger";

const DialogPortal = React.memo(({ children }: { children: React.ReactNode }) => {
  return <>{children}</>;
});

DialogPortal.displayName = "DialogPortal";

const DialogClose = React.memo(({
  children,
  onClick,
  ...props
}: React.ComponentProps<"button">) => {
  return (
    <button {...props} onClick={onClick}>
      {children || <X className="h-4 w-4" />}
    </button>
  );
});

DialogClose.displayName = "DialogClose";

const DialogOverlay = React.memo(() => null);
DialogOverlay.displayName = "DialogOverlay";

interface DialogContentProps {
  className?: string;
  children?: React.ReactNode;
  showCloseButton?: boolean;
  open?: boolean;
  onClose?: () => void;
  popup?: boolean;
  size?: "sm" | "md" | "lg" | "xl" | "2xl" | "3xl" | "4xl" | "5xl" | "6xl" | "7xl";
}

const DialogContent = React.memo(({
  className,
  children,
  showCloseButton = true,
  open,
  onClose,
  popup = false,
  size = "md",
  ...props
}: DialogContentProps) => {
  if (!open) return null;

  const childrenArray = React.Children.toArray(children);
  const header = childrenArray.find(
    (child) => React.isValidElement(child) && child.type === DialogHeader
  );
  const footer = childrenArray.find(
    (child) => React.isValidElement(child) && child.type === DialogFooter
  );
  const otherChildren = childrenArray.filter(
    (child) => !(React.isValidElement(child) &&
      (child.type === DialogHeader || child.type === DialogFooter))
  );

  return (
    <Modal
      show={open}
      onClose={() => onClose?.()}
      size={size}
      popup={popup}
      dismissible
      className={cn("", className)}
      {...props}
    >
      {showCloseButton && (
        <ModalHeader className="border-b-0">
          {header}
        </ModalHeader>
      )}
      {!showCloseButton && header && (
        <div className="p-6 pb-0">
          {header}
        </div>
      )}

      <ModalBody className="pt-0">
        {otherChildren}
      </ModalBody>

      {footer && (
        <ModalFooter className="border-t-0">
          {footer}
        </ModalFooter>
      )}
    </Modal>
  );
});

DialogContent.displayName = "DialogContent";

const DialogHeader = React.memo(({
  className,
  children,
  ...props
}: React.ComponentProps<"div">) => {
  const childrenArray = React.Children.toArray(children);
  const title = childrenArray.find(
    (child) => React.isValidElement(child) && child.type === DialogTitle
  );
  const description = childrenArray.find(
    (child) => React.isValidElement(child) && child.type === DialogDescription
  );

  return (
    <div
      className={cn("flex flex-col gap-2", className)}
      {...props}
    >
      {title}
      {description}
    </div>
  );
});

DialogHeader.displayName = "DialogHeader";

const DialogFooter = React.memo(({
  className,
  ...props
}: React.ComponentProps<"div">) => {
  return (
    <div
      className={cn(
        "flex flex-col-reverse gap-2 sm:flex-row sm:justify-end",
        className
      )}
      {...props}
    />
  );
});

DialogFooter.displayName = "DialogFooter";

const DialogTitle = React.memo(({
  className,
  ...props
}: React.ComponentProps<"h3">) => {
  return (
    <h3
      className={cn("text-lg leading-6 font-semibold text-gray-900", className)}
      {...props}
    />
  );
});

DialogTitle.displayName = "DialogTitle";

const DialogDescription = React.memo(({
  className,
  ...props
}: React.ComponentProps<"p">) => {
  return (
    <p
      className={cn("text-sm text-gray-500", className)}
      {...props}
    />
  );
});

DialogDescription.displayName = "DialogDescription";

export {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
  DialogTrigger,
}
