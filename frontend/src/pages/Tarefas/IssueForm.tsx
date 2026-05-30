import { useEffect, useState } from "react";
import { httpClient } from "api";
import { useUI } from "context";
import { Check, User, Tag } from "@phosphor-icons/react";
import type { GithubLabel, MemberBoard } from "types";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface IssueFormProps {
  action?: "create" | "update";
  issue?: any;

  boardId: number;
  columnId: number;
  repository: string;
  githubOwnerName: string;

  members: MemberBoard[];

  refetch: () => void;
  onClose?: () => void;
}

export function IssueForm({
  action = "create",
  issue,
  boardId,
  columnId,
  repository,
  githubOwnerName,
  members,
  refetch,
  onClose,
}: IssueFormProps) {
  const ui = useUI();

  const [title, setTitle] = useState(issue?.nome || "");
  const [description, setDescription] = useState(issue?.descricao || "");
  const [assignees, setAssignees] = useState<string[]>(
    issue?.assignees?.map((a: any) => a.login) || [],
  );
  const [openMembers, setOpenMembers] = useState(false);
  const [labels, setLabels] = useState<string[]>(
    issue?.labels?.map((l: any) => l.name) || [],
  );
  const [availableLabels, setAvailableLabels] = useState<GithubLabel[]>([]);
  const [openLabels, setOpenLabels] = useState(false);
  const [loading, setLoading] = useState(false);
  const [previewMode, setPreviewMode] = useState(false);

  useEffect(() => {
    if (issue?.labels) {
      setLabels(issue.labels.map((l: any) => l.name));
    }
  }, [issue]);

  useEffect(() => {
    async function loadLabels() {
      try {
        const response = await httpClient.get<GithubLabel[]>(
          `/v1/github/repository/labels?ownerName=${githubOwnerName}&repository=${repository}`,
        );

        setAvailableLabels(response);
      } catch (err) {
        console.error("Erro ao buscar labels:", err);
      }
    }

    loadLabels();
  }, [repository]);

  function toggleAssignee(login: string) {
    setAssignees((prev) =>
      prev.includes(login) ? prev.filter((a) => a !== login) : [...prev, login],
    );
  }

  function toggleLabel(name: string) {
    setLabels((prev) =>
      prev.includes(name) ? prev.filter((l) => l !== name) : [...prev, name],
    );
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    setLoading(true);

    try {
      if (action === "create") {
        await httpClient.post(
          `/v1/kanban/board/issue/create?boardId=${boardId}&columnId=${columnId}&repository=${repository}`,
          {
            title,
            body: description,
            assignees,
            labels,
          },
        );
      } else {
        await httpClient.patch(`/v1/kanban/board/issue/${issue.id}`, {
          title,
          body: description,
          assignees,
          labels,
        });
      }

      await refetch();

      onClose?.();

      ui.hide(
        "modal",
        action === "create" ? "issue-form-create" : "issue-form-update",
      );
    } catch (err) {
      console.error(
        action === "create" ? "Erro ao criar issue:" : "Erro ao editar issue:",
        err,
      );
    } finally {
      setLoading(false);
    }
  }

  function handleClose() {
    onClose?.();
    ui.hide(
      "modal",
      action === "create" ? "issue-form-create" : "issue-form-edit",
    );
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col h-full gap-4 pt-1 pb-4 px-1 min-h-[600px]"
    >
      {/* Title */}
      <div className="flex flex-col gap-1.5 shrink-0">
        <label className="text-[13px] font-semibold text-foreground">
          Add a title <span className="text-red-500">*</span>
        </label>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Title"
          required
          autoFocus
          className="w-full px-3 py-2 bg-card border border-border rounded-md outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition text-[14px]"
        />
      </div>

      {/* Description */}
      <div className="flex flex-col gap-1.5 flex-1 min-h-[300px]">
        <label className="text-[13px] font-semibold text-foreground">
          Add a description
        </label>

        <div className="flex flex-col flex-1 border border-border rounded-md bg-card overflow-hidden focus-within:border-blue-500 focus-within:ring-1 focus-within:ring-blue-500 transition">
          {/* Tabs header */}
          <div className="flex items-center gap-1 bg-secondary/50 border-b border-border px-2 py-1.5">
            <button
              type="button"
              onClick={() => setPreviewMode(false)}
              className={`px-3 py-1.5 text-[13px] font-medium rounded-md transition-colors ${
                !previewMode
                  ? "bg-card text-foreground shadow-sm border border-border"
                  : "text-muted-foreground hover:text-foreground border border-transparent"
              }`}
            >
              Write
            </button>
            <button
              type="button"
              onClick={() => setPreviewMode(true)}
              className={`px-3 py-1.5 text-[13px] font-medium rounded-md transition-colors ${
                previewMode
                  ? "bg-card text-foreground shadow-sm border border-border"
                  : "text-muted-foreground hover:text-foreground border border-transparent"
              }`}
            >
              Preview
            </button>
          </div>

          {/* Editor/Preview */}
          <div className="flex-1 relative bg-card">
            {!previewMode ? (
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Type your description here..."
                className="absolute inset-0 w-full h-full p-4 bg-transparent text-[13px] text-foreground font-mono outline-none resize-none"
              />
            ) : (
              <div
                className="absolute inset-0 w-full h-full p-5 overflow-y-auto bg-card
                prose prose-sm max-w-none
                prose-headings:text-foreground prose-headings:font-bold prose-headings:border-b prose-headings:border-border prose-headings:pb-2 prose-headings:mb-3
                prose-p:text-foreground/80 prose-p:my-2.5 prose-p:leading-relaxed
                prose-a:text-blue-600 hover:prose-a:underline prose-a:font-medium
                prose-code:bg-secondary prose-code:px-1.5 prose-code:py-0.5 prose-code:rounded-md prose-code:text-foreground prose-code:text-[13px] prose-code:font-mono prose-code:before:content-none prose-code:after:content-none
                prose-pre:bg-secondary prose-pre:text-foreground prose-pre:p-4 prose-pre:rounded-xl prose-pre:border prose-pre:border-border
                prose-ul:my-3 prose-ol:my-3 prose-ul:pl-5 prose-ol:pl-5
                prose-li:text-foreground/80 prose-li:my-1
                prose-strong:text-foreground prose-strong:font-semibold
                prose-blockquote:border-l-4 prose-blockquote:border-border prose-blockquote:text-muted-foreground prose-blockquote:pl-4 prose-blockquote:italic"
              >
                {description ? (
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>
                    {description}
                  </ReactMarkdown>
                ) : (
                  <p className="text-muted-foreground italic not-prose text-sm">
                    Nothing to preview
                  </p>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Bottom Controls */}
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mt-auto shrink-0 pt-2">
        <div className="flex items-center gap-2">
          {/* Assignees Button & Dropdown */}
          <div className="relative">
            <button
              type="button"
              onClick={() => {
                setOpenMembers(!openMembers);
                setOpenLabels(false);
              }}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-border bg-secondary hover:bg-accent text-[12px] font-medium text-foreground transition-colors"
            >
              <User size={14} className="text-muted-foreground" /> Assignee
              {assignees.length > 0 && (
                <span className="flex items-center justify-center bg-foreground text-background text-[10px] rounded-full min-w-[16px] h-[16px] px-1 font-bold">
                  {assignees.length}
                </span>
              )}
            </button>
            {openMembers && (
              <div className="absolute bottom-full mb-2 left-0 w-64 bg-card border border-border rounded-xl shadow-xl overflow-hidden z-20 animate-fade-in">
                <div className="px-3 py-2 border-b border-border bg-secondary/50">
                  <span className="text-xs font-semibold text-foreground">
                    Assign up to 10 people
                  </span>
                </div>
                <div className="max-h-48 overflow-y-auto py-1">
                  {members.length === 0 ? (
                    <p className="px-4 py-3 text-sm text-muted-foreground">
                      Nenhum membro encontrado
                    </p>
                  ) : (
                    members.map((member) => {
                      const selected = assignees.includes(member.login);
                      return (
                        <button
                          key={member.login}
                          type="button"
                          onClick={() => toggleAssignee(member.login)}
                          className="w-full flex items-center gap-2 px-3 py-2 hover:bg-secondary transition text-left"
                        >
                          <div className="w-4 flex justify-center shrink-0">
                            {selected && (
                              <Check
                                size={14}
                                weight="bold"
                                className="text-foreground"
                              />
                            )}
                          </div>
                          <span className="text-sm text-foreground font-medium truncate">
                            {member.login}
                          </span>
                        </button>
                      );
                    })
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Labels Button & Dropdown */}
          <div className="relative">
            <button
              type="button"
              onClick={() => {
                setOpenLabels(!openLabels);
                setOpenMembers(false);
              }}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-full border border-border bg-secondary hover:bg-accent text-[12px] font-medium text-foreground transition-colors"
            >
              <Tag size={14} className="text-muted-foreground" /> Label
              {labels.length > 0 && (
                <span className="flex items-center justify-center bg-foreground text-background text-[10px] rounded-full min-w-[16px] h-[16px] px-1 font-bold">
                  {labels.length}
                </span>
              )}
            </button>
            {openLabels && (
              <div className="absolute bottom-full mb-2 left-0 w-64 bg-card border border-border rounded-xl shadow-xl overflow-hidden z-20 animate-fade-in">
                <div className="px-3 py-2 border-b border-border bg-secondary/50">
                  <span className="text-xs font-semibold text-foreground">
                    Apply labels
                  </span>
                </div>
                <div className="max-h-48 overflow-y-auto py-1">
                  {availableLabels.length === 0 ? (
                    <p className="px-4 py-3 text-sm text-muted-foreground">
                      Nenhuma label encontrada
                    </p>
                  ) : (
                    availableLabels.map((label) => {
                      const selected = labels.includes(label.name);
                      return (
                        <button
                          key={label.name}
                          type="button"
                          onClick={() => toggleLabel(label.name)}
                          className="w-full flex items-center gap-2 px-3 py-2 hover:bg-secondary transition text-left"
                        >
                          <div className="w-4 flex justify-center shrink-0">
                            {selected && (
                              <Check
                                size={14}
                                weight="bold"
                                className="text-foreground"
                              />
                            )}
                          </div>
                          <span
                            className="w-3 h-3 rounded-full shrink-0 border border-black/10"
                            style={{ backgroundColor: `#${label.color}` }}
                          />
                          <span className="text-sm text-foreground font-medium truncate">
                            {label.name}
                          </span>
                        </button>
                      );
                    })
                  )}
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
          <button
            type="button"
            onClick={handleClose}
            className="px-3 py-1.5 text-[13px] font-semibold text-foreground bg-secondary border border-border hover:bg-accent rounded-md transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading}
            className="px-3 py-1.5 text-[13px] font-semibold text-white bg-primary hover:bg-[#1a2f7a] border border-primary rounded-md transition-colors disabled:opacity-50 flex items-center gap-1.5"
          >
            {loading
              ? "Saving..."
              : action === "create"
                ? "Create issue"
                : "Save changes"}
          </button>
        </div>
      </div>
    </form>
  );
}
