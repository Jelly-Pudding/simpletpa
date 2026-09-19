package com.jellypudding.simpleTPA.request;

import java.util.UUID;

public record TeleportRequest(UUID requester, UUID target, RequestType type) {
}
