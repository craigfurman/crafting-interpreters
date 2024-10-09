defmodule PeekableStream do
  def new(stream) do
    spawn_link(fn -> loop(stream, read_char(stream)) end)
  end

  def next(pid), do: sendMsg(pid, :next)
  def peek(pid), do: sendMsg(pid, :peek)
  def stop(pid), do: send(pid, :stop)

  defp sendMsg(pid, kind) do
    send(pid, {kind, self()})

    receive do
      {^kind, ^pid, char} -> char
    end
  end

  defp loop(stream, buffer) do
    receive do
      {:next, caller} ->
        send(caller, {:next, self(), buffer})
        loop(stream, read_char(stream))

      {:peek, caller} ->
        send(caller, {:peek, self(), buffer})
        loop(stream, buffer)

      :stop ->
        :ok
    end
  end

  defp read_char(stream) do
    case Stream.take(stream, 1) |> Enum.to_list() do
      [] -> :eof
      [char] -> char
    end
  end
end
