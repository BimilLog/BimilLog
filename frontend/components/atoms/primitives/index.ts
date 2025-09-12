// Legacy UI compatibility layer
export * from "./button";
export * from "./card";
export * from "./input";
export * from "./textarea";
export * from "./alert";

// Re-export from atoms/molecules for compatibility
export { Badge } from "../display/badge";
export { Label } from "../forms/label";
export { Switch } from "../actions/switch";
export { Avatar, AvatarImage, AvatarFallback } from "../display/avatar";

// Re-export from molecules
export { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "../../molecules/modals/dialog";
export { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle, SheetTrigger } from "../../molecules/modals/sheet";
export { Popover, PopoverContent, PopoverTrigger } from "../../molecules/modals/popover";
export { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "../../molecules/dropdown-menu";
export { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../../molecules/select";
export { Tabs, TabsList, TabsTrigger, TabsContent } from "../../molecules/tabs";