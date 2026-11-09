#!/usr/bin/env bash
# Merges JetBrains release tags into the fork's main branch.
# Process description: docs/upstream-sync.md
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"

BASE_FILE="fork/UPSTREAM_BASE"
UPSTREAM="${UPSTREAM_REMOTE:-upstream}"
MAIN="${MAIN_BRANCH:-main}"

die() { echo "error: $*" >&2; exit 1; }

usage() {
  cat <<EOF
Usage: fork/upstream-sync.sh <command> [tag]

  status          Show the current base tag and newer upstream release tags.
  check           Print the newest release tag of the base series if it is newer
                  than the base. Print nothing if the base is up to date.
  merge [tag]     Merge an upstream tag into a new sync/<version> branch.
                  Default tag: the newest tag of the base series.
  finish <tag>    Record the tag in $BASE_FILE and commit the merge
                  (after you resolve the conflicts).
  abort           Cancel an open merge and delete its sync branch.

Environment: UPSTREAM_REMOTE (default: upstream), MAIN_BRANCH (default: main).
EOF
}

current_base() {
  [[ -f "$BASE_FILE" ]] || die "$BASE_FILE is missing. Write the base tag into it (for example idea/263.7307.200)."
  tr -d '[:space:]' < "$BASE_FILE"
}

# idea/263.7307.200 -> 263
series_of() { local v="${1#idea/}"; echo "${v%%.*}"; }

remote_tags() {
  git ls-remote --tags --refs "$UPSTREAM" "refs/tags/idea/$1.*" \
    | awk '{ sub("refs/tags/", "", $2); print $2 }' | sort -V
}

newest_series() {
  git ls-remote --heads "$UPSTREAM" \
    | awk '{ sub("refs/heads/", "", $2); print $2 }' \
    | grep -E '^[0-9]{3}$' | sort -n | tail -1
}

# Tags of the series that sort after the base tag.
newer_tags() {
  local base="$1"
  remote_tags "$(series_of "$base")" | awk -v b="$base" 'found { print } $0 == b { found = 1 }'
}

branch_for() { echo "sync/${1#idea/}"; }

require_clean_tree() {
  git diff --quiet && git diff --cached --quiet \
    || die "the working tree has uncommitted changes. Commit or stash them first."
}

cmd_status() {
  local base series newest
  base="$(current_base)"
  series="$(series_of "$base")"
  echo "Base tag:        $base"
  echo "Base series:     $series"
  newest="$(newest_series)"
  echo "Newest series:   $newest"
  if [[ "$newest" != "$series" ]]; then
    echo "A newer release series is available ($newest). This is a major update. Read docs/upstream-sync.md first."
  fi
  echo "Newer tags in series $series:"
  newer_tags "$base" | sed 's/^/  /' || true
}

cmd_check() {
  newer_tags "$(current_base)" | tail -1
}

# The result of a sync is the upstream tree plus the fork changes, and nothing else.
# A plain `git merge` does not give this result. A new upstream build line does not contain the commits of the
# previous release tag, so the merge base is older than the base tag. The merge then applies the tag-only commits
# a second time, and files that depend on each other no longer fit.
# So the sync takes the upstream tree and applies the fork diff (base tag -> main) with a three-way apply.
# The commit has two parents, main and the tag, so the history shows the merge.
apply_fork_onto() {
  local upstream_ref="$1" base="$2"
  git merge --no-ff --no-commit -s ours "$upstream_ref" >/dev/null
  git read-tree --reset -u "$upstream_ref"
  git diff --binary --full-index "$base" "$MAIN" | git apply --3way --index --whitespace=nowarn
}

cmd_merge() {
  local tag="${1:-}" base branch
  base="$(current_base)"
  [[ -n "$tag" ]] || tag="$(newer_tags "$base" | tail -1)"
  [[ -n "$tag" ]] || { echo "The base $base is up to date. Nothing to merge."; return 0; }

  require_clean_tree
  branch="$(branch_for "$tag")"
  git rev-parse --verify --quiet "$MAIN" >/dev/null || die "branch $MAIN does not exist."
  git rev-parse --verify --quiet "$branch" >/dev/null && die "branch $branch already exists. Use 'abort' or delete it."

  echo "Fetching $tag and $base from $UPSTREAM..."
  git fetch --no-tags "$UPSTREAM" "refs/tags/$tag:refs/tags/$tag" "refs/tags/$base:refs/tags/$base"

  if git merge-base --is-ancestor "$tag" "$MAIN"; then
    echo "$tag is already merged into $MAIN."
    return 0
  fi

  git switch -c "$branch" "$MAIN"
  echo "Applying the fork changes ($base..$MAIN) onto $tag in $branch..."
  if apply_fork_onto "$tag" "$base"; then
    cmd_finish "$tag"
  else
    echo
    echo "Conflicts in the files that the fork changed:"
    git diff --name-only --diff-filter=U | sed 's/^/  /'
    echo
    echo "Resolve the conflicts, 'git add' the files, then run:"
    echo "  fork/upstream-sync.sh finish $tag"
    echo "To cancel: fork/upstream-sync.sh abort"
    exit 1
  fi
}


cmd_finish() {
  local tag="${1:-}"
  [[ -n "$tag" ]] || die "finish needs the tag (for example idea/263.7307.200)."
  git rev-parse --verify --quiet MERGE_HEAD >/dev/null || die "no merge is in progress."
  [[ -z "$(git diff --name-only --diff-filter=U)" ]] || die "there are unresolved conflicts."

  echo "$tag" > "$BASE_FILE"
  git add "$BASE_FILE"
  git -c rerere.enabled=true commit --no-edit -m "fork: merge upstream $tag"
  echo
  echo "Merged $tag on branch $(git branch --show-current). Next steps:"
  echo "  1. Move the android/ checkout to the same tag: git -C android fetch origin refs/tags/$tag && git -C android switch --detach FETCH_HEAD"
  echo "  2. Build and run the IDE (see docs/upstream-sync.md, 'Checks')."
  echo "  3. Push the branch and open a pull request into $MAIN."
}

cmd_abort() {
  local branch
  branch="$(git branch --show-current)"
  [[ "$branch" == sync/* ]] || die "the current branch $branch is not a sync branch."
  git merge --abort 2>/dev/null || true
  git switch "$MAIN"
  git branch -D "$branch"
}

case "${1:-}" in
  status) cmd_status ;;
  check)  cmd_check ;;
  merge)  cmd_merge "${2:-}" ;;
  finish) cmd_finish "${2:-}" ;;
  abort)  cmd_abort ;;
  ""|-h|--help|help) usage ;;
  *) usage; exit 1 ;;
esac
