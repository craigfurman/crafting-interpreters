# TODO probably better as an Agent, but I'm learning ok
defmodule Counter do
  def start(initial_value \\ 0) do
    spawn_link(fn -> loop(initial_value) end)
  end

  def increment(pid) do
    send(pid, :increment)
  end

  def value(pid) do
    send(pid, {:value, self()})

    receive do
      {:value, ^pid, value} -> value
    end
  end

  def stop(pid) do
    send(pid, :stop)
  end

  defp loop(value) do
    receive do
      {:value, caller} ->
        send(caller, {:value, self(), value})
        loop(value)

      :increment ->
        loop(value + 1)

      :stop ->
        :ok
    end
  end
end
