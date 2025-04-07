'use client';

import * as React from 'react';
import { History, PlusCircle, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

import { NavMain } from '@/components/sidebar/NavMain';
import { NavUser } from '@/components/sidebar/NavUser';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
  SidebarMenuButton
} from '@/components/ui/Sidebar';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { useConversation } from '@/contexts/ConversationContext';

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const {
    conversations,
    activeConversationId,
    setActiveConversationId,
    createConversation,
    deleteConversation
  } = useConversation();
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = React.useState(false);
  const [conversationToDelete, setConversationToDelete] = React.useState<string | null>(null);
  
  const handleConfirmDelete = async () => {
    if (!conversationToDelete) return;
    
    const success = await deleteConversation(conversationToDelete);
    if (success) {
      toast.success('Conversation deleted');
    } else {
      toast.error('Failed to delete conversation');
    }
    
    setIsDeleteDialogOpen(false);
    setConversationToDelete(null);
  };

  const navMainData = [
    {
      title: 'History',
      url: '#',
      icon: History,
      isActive: true,
      className: 'bg-transparent',
      items: conversations.map((conversation) => ({
        title: conversation.name,
        url: '#',
        id: conversation.id,
        isActive: activeConversationId === conversation.id,
        onClick: () => setActiveConversationId(conversation.id),
        actions: [
          {
            icon: Trash2,
            className: 'text-muted-foreground hover:text-destructive',
            onClick: (e: React.MouseEvent) => {
              e.stopPropagation();
              setConversationToDelete(conversation.id);
              setIsDeleteDialogOpen(true);
            }
          }
        ]
      })),
    },
  ];

  return (
    <>
      <Sidebar collapsible="icon" className="bg-background/85" {...props}>
        <SidebarHeader></SidebarHeader>
        <SidebarContent>
          <div className="relative flex w-full min-w-0 flex-col p-2">
            <SidebarMenuButton 
                data-testid="new-chat-button"
                onClick={createConversation}
                tooltip={"New Chat"} 
                className={"bg-transparent w-full justify-center items-center flex min-w-0 gap-1 border border-input group-data-[collapsible=icon]:border-0"}>
                    {PlusCircle && <PlusCircle className=""/>}
                    <span>{"New Chat"}</span>
                
            </SidebarMenuButton>
          </div>
          <NavMain items={navMainData} />
        </SidebarContent>
        <SidebarFooter>
          <NavUser />
        </SidebarFooter>
        <SidebarRail />
      </Sidebar>
      
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Delete Conversation</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete this conversation? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button 
              variant="outline" 
              onClick={() => setIsDeleteDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button 
              variant="destructive" 
              onClick={handleConfirmDelete}
              className="text-white"
            >
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
