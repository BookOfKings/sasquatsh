import SwiftUI

struct ChatPanelView: View {
    let contextType: String
    let contextId: String

    @Environment(\.services) private var services
    @Environment(AuthViewModel.self) private var authVM
    @State private var vm = ChatViewModel()
    @State private var reportingMessage: ChatMessage?

    var body: some View {
        VStack(spacing: 0) {
            if vm.isLoading && vm.messages.isEmpty {
                Spacer()
                LoadingView()
                Spacer()
            } else if vm.messages.isEmpty {
                Spacer()
                VStack(spacing: 12) {
                    Image(systemName: "bubble.left.and.bubble.right")
                        .font(.system(size: 40))
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text("No messages yet")
                        .font(.md3BodyLarge)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                    Text("Start the conversation!")
                        .font(.md3BodySmall)
                        .foregroundStyle(Color.md3OnSurfaceVariant)
                }
                Spacer()
            } else {
                messagesListView
            }

            if let error = vm.error {
                ErrorBannerView(message: error) { vm.error = nil }
            }

            chatInputBar
        }
        .task {
            vm.configure(services: services, contextType: contextType, contextId: contextId)
            await vm.loadMessages()
            vm.startPolling()
        }
        .onDisappear {
            vm.stopPolling()
        }
        .sheet(item: $reportingMessage) { message in
            ReportMessageSheet(message: message) { reason, details in
                Task {
                    await vm.reportMessage(message.id, reason: reason, details: details)
                }
            }
        }
    }

    // MARK: - Messages List

    private var messagesListView: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 8) {
                    if vm.hasMore {
                        Button {
                            Task { await vm.loadEarlierMessages() }
                        } label: {
                            Text("Load earlier messages")
                                .font(.md3LabelMedium)
                                .foregroundStyle(Color.md3Primary)
                        }
                        .padding(.top, 8)
                    }

                    ForEach(vm.messages) { message in
                        ChatBubbleView(
                            message: message,
                            isOwnMessage: message.userId == authVM.user?.id,
                            onDelete: {
                                Task { await vm.deleteMessage(message.id) }
                            },
                            onReport: {
                                reportingMessage = message
                            }
                        )
                        .id(message.id)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
            }
            .onChange(of: vm.messages.count) { _, _ in
                if let lastId = vm.messages.last?.id {
                    withAnimation {
                        proxy.scrollTo(lastId, anchor: .bottom)
                    }
                }
            }
        }
    }

    // MARK: - Input Bar

    private var chatInputBar: some View {
        HStack(spacing: 8) {
            TextField("Message...", text: $vm.messageText, axis: .vertical)
                .lineLimit(1...4)
                .textFieldStyle(.roundedBorder)
                .font(.md3BodyMedium)
                .disabled(vm.isSending)
                .onChange(of: vm.messageText) { _, newValue in
                    if newValue.count > 1000 {
                        vm.messageText = String(newValue.prefix(1000))
                    }
                }

            Button {
                Task { await vm.sendMessage() }
            } label: {
                Image(systemName: "arrow.up.circle.fill")
                    .font(.title2)
                    .foregroundStyle(canSend ? Color.md3Primary : Color.md3OnSurfaceVariant)
            }
            .disabled(!canSend)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Color.md3SurfaceContainer)
    }

    private var canSend: Bool {
        !vm.messageText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !vm.isSending
    }
}
