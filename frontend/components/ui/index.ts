// Legacy UI compatibility layer
export * from "./button";
export * from "./card";
export * from "./input";
export * from "./textarea";
export * from "./alert";

// Re-export from atoms/molecules for compatibility
export { Badge } from "../atoms/display/badge";
export { Label } from "../atoms/forms/label";
export { Switch } from "../atoms/actions/switch";
export { Avatar, AvatarImage, AvatarFallback } from "../atoms/display/avatar";

// Re-export from molecules
export { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../molecules/modals/dialog";
export { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle, SheetTrigger } from "../molecules/modals/sheet";
export { Popover, PopoverContent, PopoverTrigger } from "../molecules/modals/popover";
export { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "../molecules/dropdown-menu";
export { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../molecules/select";
export { Tabs, TabsList, TabsTrigger, TabsContent } from "../molecules/tabs";