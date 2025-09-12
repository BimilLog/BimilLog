// Legacy compatibility export  
export { Button } from "../atoms/actions/button";

// For compatibility with the buttonVariants export
import { Button as ButtonComponent } from "../atoms/actions/button";
export const buttonVariants = (ButtonComponent as any)?.variants || {};